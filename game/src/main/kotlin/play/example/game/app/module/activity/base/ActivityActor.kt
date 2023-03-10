package play.example.game.app.module.activity.base

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import mu.KLogging
import play.akka.AbstractTypedActor
import play.akka.scheduling.ActorScheduler
import play.akka.scheduling.WithActorScheduler
import play.example.game.app.module.activity.base.entity.ActivityEntityCache
import play.example.game.app.module.activity.base.event.ActivityStartPlayerEvent
import play.example.game.app.module.activity.base.res.ActivityResource
import play.example.game.app.module.activity.base.res.ActivityResourceSet
import play.example.game.app.module.activity.base.stage.ActivityStage
import play.example.game.app.module.activity.base.stage.ActivityStageHandler
import play.example.game.app.module.activity.base.trigger.ActivityTimeTriggerContext
import play.example.game.app.module.player.OnlinePlayerService
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.event.PlayerEventBus
import play.example.game.app.module.player.event.PlayerTaskEventLike
import play.example.game.app.module.player.event.subscribe
import play.example.game.app.module.server.ServerConditionService
import play.util.concurrent.PlayPromise
import play.util.concurrent.Promise
import play.util.control.getCause
import play.util.time.Time
import kotlin.time.Duration.Companion.seconds

/**
 *
 * @author LiangZengle
 */
class ActivityActor(
  ctx: ActorContext<Command>,
  private val id: Int,
  private val manager: ActorRef<ActivityManager.Command>,
  val triggerContext: ActivityTimeTriggerContext,
  private val entityCache: ActivityEntityCache,
  override val actorScheduler: ActorRef<ActorScheduler.Command>,
  val handler: ActivityHandler,
  val serverConditionService: ServerConditionService,
  private val activityCache: ActivityCache,
  private val playerEventBus: PlayerEventBus,
  private val playerActivityService: PlayerActivityService
) : AbstractTypedActor<ActivityActor.Command>(ctx),
  WithActorScheduler<ActivityActor.Command> {

  /**
   * ????????????????????????
   */
  private var onClosed: PlayPromise<Unit>? = null

  /**
   * ??????????????????????????????????????????????????????
   */
  private var currentStage = ActivityStage.None

  /**
   * ?????????????????????????????????????????????????????????
   */
  private var resource = ActivityResourceSet.getOrThrow(id)

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::init)
      .build()
  }

  private fun initialized() = newBehaviorBuilder()
    .accept<ActivityNotice>(::notice)
    .accept<ActivityStart>(::start)
    .accept<ActivityEnd>(::end)
    .accept<ActivityClose>(::close)
    .accept<JustClose>(::justClose)
    .accept(::eventTriggered)
    .accept(::suspend)
    .accept(::resume)
    .accept<CheckResourceReload>(::checkResourceReload)
    .accept(::forceClose)
    .build()

  private fun getEntity() = entityCache.getOrThrow(id)

  private fun init(cmd: Init): Behavior<Command> {
    cancelAllSchedules()
    var entity = entityCache.getOrNull(id)

    // ????????????????????????
    fun isOutdated(resource: ActivityResource): Boolean {
      if (triggerContext.isForeverOpen(resource.startTime)) {
        return false
      }
      if (triggerContext.isForeverClose(resource.startTime)) {
        return true
      }
      if (triggerContext.isForeverOpen(resource.startTime)) {
        return false
      }
      val now = Time.currentDateTime()
      val startTime = resource.startTime.nextTriggerTime(now, triggerContext)
      val endTime = startTime?.plus(resource.duration)
      return startTime == null || endTime == null || endTime < now
    }

    if (entity == null && !isOutdated(resource)) {
      entity = entityCache.getOrCreate(id)
    }
    if (entity != null) {
      refreshStage()
    }
    if (handler is ActivityTaskEventHandler) {
      playerEventBus.subscribe(::onTaskEvent)
    }
    manager.tell(ActivityManager.ActivityInitialized(resource.id))
    return initialized()
  }

  private fun checkResourceReload() {
    val newResource = ActivityResourceSet.getOrThrow(id)
    if (newResource.version == resource.version) {
      return
    }
    this.resource = newResource
    val entity = entityCache.getOrNull(id)
    if (entity == null) {
      init(Init)
    } else {
      entity.stage.handler.reload(entity, resource)
    }
  }

  private fun forceClose(cmd: ForceClose) {
    logger.info { "??????[$id]????????????" }
    val promise = cmd.promise
    if (currentStage == ActivityStage.Close || currentStage == ActivityStage.None) {
      promise.success(Unit)
      return
    }
    val entity = entityCache.getOrNull(id)
    if (entity == null) {
      promise.success(Unit)
      return
    }
    val prev = onClosed
    val newClosePromise = Promise.make<Unit>()
    onClosed = newClosePromise

    // cancel all schedules
    cancelAllSchedules()

    var stage: ActivityStage? = entity.stage
    while (stage != null) {
      if (stage == ActivityStage.Start) {
        end()
      } else if (stage == ActivityStage.End) {
        close()
        break
      }
      stage = stage.next()
    }

    prev?.completeWith(newClosePromise.future)
    promise.completeWith(newClosePromise.future)

    // refresh
    newClosePromise.future.pipToSelf { Refresh }
  }

  private fun notice() {
    handler.onNotice(resource)
  }

  private fun start() {
    startStage(ActivityStage.Start)
    if (!ActivityResourceSet.extension().getChildActivityIds(id).isEmpty) {
      manager.tell(ActivityManager.StartChildren(id))
    }
    playerActivityService.onActivityStart(id)
  }

  private fun end() {
    startStage(ActivityStage.End)
  }

  private fun close() {
    if (ActivityResourceSet.extension().getChildActivityIds(id).isEmpty) {
      justClose()
    } else {
      val promise = PlayPromise.make<Unit>()
      manager.tell(ActivityManager.CloseChildren(id, promise))
      promise.future.timeout(30.seconds).pipToSelf {
        if (it.isFailure) {
          log.error("activity({}) failed to close child activities in 30s", id, it.getCause())
        }
        JustClose
      }
    }
  }

  private fun justClose() {
    startStage(ActivityStage.Close)
    playerActivityService.onActivityClose(id)
  }

  private fun startStage(stage: ActivityStage) {
    val entity = getEntity()
    if (entity.stage.next() != stage) {
      logger.warn { "activity($id) can not start stage `$stage`??? current stage is ${entity.stage}" }
      return
    }
    try {
      stage.handler.start(entity, resource)
    } finally {
      currentStage = entity.stage
      activityCache.update(id, currentStage)
      if (stage == ActivityStage.Close) {
        onClosed?.success(Unit)
        onClosed = null
      }
    }
  }

  private fun refreshStage() {
    val entity = getEntity()
    try {
      entity.stage.handler.refresh(entity, resource)
    } finally {
      currentStage = entity.stage
      activityCache.update(id, currentStage)
    }
  }

  private fun onTaskEvent(self: PlayerManager.Self, event: PlayerTaskEventLike) {
    if (currentStage == ActivityStage.Start && handler is ActivityTaskEventHandler) {
      handler.onTaskEvent(self, event, playerActivityService.getOrCreateData(self, id), getEntity(), resource)
    }
  }

  private fun eventTriggered(event: ActivityTriggerEvent) {
    val entity = getEntity()
    val handler = entity.stage.handler
    if (handler is ActivityStageHandler.EventTriggerable) {
      handler.eventTriggered(event.name, entity, resource)
    }
  }

  private fun suspend(cmd: ActivitySuspend) {
    val promise = cmd.promise
    promise.catchingComplete {
      val entity = getEntity()
      val handler = entity.stage.handler
      logger.info { "??????[$id]?????????????????????[${entity.stage}]??????" }
      if (handler is ActivityStageHandler.Suspendable) {
        handler.suspend(entity, resource)
      }
    }
  }

  private fun resume(cmd: ActivityResume) {
    val promise = cmd.promise
    promise.catchingComplete {
      val entity = getEntity()
      val handler = entity.stage.handler
      logger.info { "??????[$id]?????????????????????[${entity.stage}]??????" }
      if (handler is ActivityStageHandler.Suspendable) {
        handler.resume(entity, resource)
      }
    }
  }

  companion object : KLogging() {
    fun create(
      id: Int,
      manager: ActorRef<ActivityManager.Command>,
      triggerContext: ActivityTimeTriggerContext,
      entityCache: ActivityEntityCache,
      actorScheduler: ActorRef<ActorScheduler.Command>,
      handler: ActivityHandler,
      serverConditionService: ServerConditionService,
      activityCache: ActivityCache,
      playerEventBus: PlayerEventBus,
      playerActivityService: PlayerActivityService
    ): Behavior<Command> {
      return Behaviors.setup { ctx ->
        ActivityActor(
          ctx,
          id,
          manager,
          triggerContext,
          entityCache,
          actorScheduler,
          handler,
          serverConditionService,
          activityCache,
          playerEventBus,
          playerActivityService
        )
      }
    }
  }

  interface Command

  object Init : Command

  object CheckResourceReload : Command

  class ForceClose(val promise: PlayPromise<Unit>) : Command

  object Refresh : Command

  data class ActivityTriggerEvent(val name: String) : Command

  object ActivityNotice : Command
  object ActivityStart : Command
  object ActivityEnd : Command
  object ActivityClose : Command
  private object JustClose : Command

  class ActivitySuspend(val promise: Promise<Unit>) : Command
  class ActivityResume(val promise: Promise<Unit>) : Command
}
