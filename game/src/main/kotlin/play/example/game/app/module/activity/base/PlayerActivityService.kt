package play.example.game.app.module.activity.base

import com.squareup.wire.AnyMessage
import org.eclipse.collections.api.factory.Lists
import org.springframework.stereotype.Component
import play.example.common.StatusCode
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.entity.PlayerActivityData
import play.example.game.app.module.activity.base.entity.PlayerActivityEntity
import play.example.game.app.module.activity.base.entity.PlayerActivityEntityCache
import play.example.game.app.module.activity.base.event.ActivityClosePlayerEvent
import play.example.game.app.module.activity.base.event.ActivityStartPlayerEvent
import play.example.game.app.module.activity.base.res.ActivityResourceSet
import play.example.game.app.module.activity.base.stage.ActivityStage
import play.example.game.app.module.activity.base.stage.ActivityStages
import play.example.game.app.module.player.OnlinePlayerService
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.condition.PlayerConditionService
import play.example.game.app.module.player.event.PlayerEventBus
import play.example.game.app.module.player.event.PlayerLoginEvent
import play.example.game.app.module.player.event.subscribe
import play.example.game.container.net.Session
import play.example.module.activity.message.ActivityListProto
import play.example.module.activity.message.ActivityProto
import play.spring.SingletonBeanContext
import play.util.control.Result2
import play.util.unsafeCast

/**
 *
 * @author LiangZengle
 */
@Component
class PlayerActivityService(
  private val activityCache: ActivityCache,
  private val playerActivityCache: PlayerActivityEntityCache,
  private val beanContext: SingletonBeanContext,
  playerEventBus: PlayerEventBus,
  private val playerConditionService: PlayerConditionService,
  private val onlinePlayerService: OnlinePlayerService
) {

  init {
    playerEventBus.subscribe<PlayerLoginEvent>(::onPlayerLogin)
    playerEventBus.subscribe<ActivityStartPlayerEvent> { self, event -> onActivityStart(self, event.activityId) }
    playerEventBus.subscribe<ActivityClosePlayerEvent> { self, event -> onActivityClose(self, event.activityId) }
  }

  fun getOrCreateData(self: PlayerManager.Self, activityId: Int): PlayerActivityData {
    return playerActivityCache.getOrCreate(self.id, ::PlayerActivityEntity).getOrCreate(activityId)
  }

  fun getData(self: PlayerManager.Self, activityId: Int): PlayerActivityData? {
    return playerActivityCache.getOrNull(self.id)?.get(activityId)
  }

  fun getHandler(activityType: ActivityType) = beanContext.getImpl(ActivityHandler::class.java, activityType)

  fun getHandler(activityId: Int): ActivityHandler {
    val activityType = ActivityResourceSet.getOrThrow(activityId).type
    return beanContext.getImpl(ActivityHandler::class.java, activityType)
  }

  fun <R> process(
    self: PlayerManager.Self,
    activityId: Int,
    action: (ActivityEntity, PlayerActivityData) -> Result2<R>
  ): Result2<R> {
    return process(self, activityId, ActivityStage.Start.toStages(), action)
  }

  fun <R> process(
    self: PlayerManager.Self,
    activityType: ActivityType,
    action: (ActivityEntity, PlayerActivityData) -> Result2<R>
  ): Result2<R> {
    return process(self, activityType, ActivityStage.Start.toStages(), action)
  }

  fun <R> process(
    self: PlayerManager.Self,
    activityId: Int,
    requireStages: ActivityStages,
    action: (ActivityEntity, PlayerActivityData) -> Result2<R>
  ): Result2<R> {
    val activityEntity = activityCache.getActivity(activityId, requireStages)
      ?: return StatusCode.Failure // todo error code
    return process(self, activityEntity, action)
  }

  fun <R> process(
    self: PlayerManager.Self,
    activityType: ActivityType,
    requireStages: ActivityStages,
    action: (ActivityEntity, PlayerActivityData) -> Result2<R>
  ): Result2<R> {
    return activityCache.getActivities(activityType, requireStages).fold(StatusCode.Failure) { r, activityEntity ->
      val result = process(self, activityEntity, action)
      if (r.isOk()) r else result.unsafeCast()
    }
  }

  private fun <R> process(
    self: PlayerManager.Self,
    activityEntity: ActivityEntity,
    action: (ActivityEntity, PlayerActivityData) -> Result2<R>
  ): Result2<R> {
    val activityId = activityEntity.id
    val playerActivityData = getOrCreateData(self, activityId)
    // 自动结算和参与
    tryBalance(self, playerActivityData)
    tryJoin(self, playerActivityData, activityEntity)

    // 参与状态
    if (!playerActivityData.isJoined()) {
      return StatusCode.Failure // todo error code
    }
    return action(activityEntity, playerActivityData)
  }

  fun onActivityStart(activityId: Int) {
    onlinePlayerService.postEventToOnlinePlayers {
      ActivityStartPlayerEvent(it, activityId)
    }
  }

  fun onActivityClose(activityId: Int) {
    onlinePlayerService.postEventToOnlinePlayers {
      ActivityClosePlayerEvent(it, activityId)
    }
  }

  private fun onActivityStart(self: PlayerManager.Self, activityId: Int) {
    val activity = activityCache.getActivity(activityId, ActivityStage.Start.toStages()) ?: return
    val playerActivityData = getOrCreateData(self, activityId)
    val joined = tryJoin(self, playerActivityData, activity)
    if (joined) {
      Session.write(self.id, ActivityModule.activityStartPush(toActivityProto(self, activity, playerActivityData)))
    }
  }

  private fun onActivityClose(self: PlayerManager.Self, activityId: Int) {
    val activityData = getData(self, activityId) ?: return
    tryBalance(self, activityData)
  }

  private fun onPlayerLogin(self: PlayerManager.Self) {
    // 活动结算检查
    tryBalanceAll(self)
    // 加入活动
    tryJoinAll(self)
  }

  private fun tryBalanceAll(self: PlayerManager.Self) {
    val entity = playerActivityCache.getOrCreate(self.id)
    for (activityData in entity.listData()) {
      tryBalance(self, activityData)
    }
  }

  private fun tryBalance(self: PlayerManager.Self, activityData: PlayerActivityData) {
    if (activityData.balanced) return
    val activity = activityCache.getActivity(activityData.activityId, ActivityStages.ANY) ?: return
    if (activity.openTimes == activityData.version || !activity.isClosed()) {
      return
    }
    val activityResource = ActivityResourceSet.getOrNull(activityData.activityId) ?: return
    val handler = getHandler(activityResource.type)
    try {
      handler.balance(self, activityData)
    } finally {
      activityData.balanced = true
    }
  }

  private fun tryJoinAll(self: PlayerManager.Self) {
    val entity = playerActivityCache.getOrCreate(self.id)
    for (activity in activityCache.getActivities(ActivityStage.Start.toStages())) {
      tryJoin(self, entity.getOrCreate(activity.id), activity)
    }
  }

  private fun tryJoin(
    self: PlayerManager.Self,
    activityData: PlayerActivityData,
    activityEntity: ActivityEntity
  ): Boolean {
    if (!activityData.isEmptyState()) {
      return false
    }
    val activityResource = ActivityResourceSet.getOrNull(activityData.activityId) ?: return false
    if (playerConditionService.check(self, activityResource.joinConditions).isErr()) {
      return false
    }
    val handler = getHandler(activityResource.type)
    activityData.join(activityEntity.openTimes)
    handler.join(self, activityData, activityEntity, activityResource)
    return true
  }

  fun listActivities(self: PlayerManager.Self): ActivityListProto {
    val activityProtoList = Lists.mutable.empty<ActivityProto>()
    for (activity in activityCache.getActivities(ActivityStages.START or ActivityStages.END)) {
      val playerActivityData = getData(self, activity.id) ?: continue
      if (playerActivityData.isJoined()) {
        continue
      }
      val activityProto = toActivityProto(self, activity, playerActivityData)
      activityProtoList.add(activityProto)
    }
    return ActivityListProto(activityProtoList)
  }

  private fun toActivityProto(
    self: PlayerManager.Self,
    activity: ActivityEntity,
    playerData: PlayerActivityData
  ): ActivityProto {
    val activityId = activity.id
    val handler = getHandler(activityId)
    val dataProto = handler.getDataProto(self, playerData)
    return ActivityProto(activityId, activity.startTime, activity.endTime, AnyMessage.pack(dataProto))
  }
}
