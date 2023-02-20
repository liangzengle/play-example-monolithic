package play.example.game.app.module.activity.impl.task

import com.squareup.wire.Message
import org.springframework.stereotype.Component
import play.example.game.app.module.activity.base.ActivityHandler
import play.example.game.app.module.activity.base.ActivityTaskEventHandler
import play.example.game.app.module.activity.base.ActivityType
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.entity.PlayerActivityData
import play.example.game.app.module.activity.base.res.ActivityResource
import play.example.game.app.module.activity.impl.task.res.TaskActivityResourceSet
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.event.PlayerTaskEventLike
import play.example.game.app.module.playertask.PlayerTaskTargetHandlerProvider
import play.example.game.app.module.reward.RewardService
import play.example.module.activity.message.TaskActivityDataProto

/**
 *
 *
 * @author LiangZengle
 */
@Component
class TaskActivityHandler(
  private val targetHandlerProvider: PlayerTaskTargetHandlerProvider,
  private val rewardService: RewardService
) : ActivityHandler, ActivityTaskEventHandler {

  override fun type(): ActivityType = ActivityType.TASK

  override fun join(
    self: PlayerManager.Self,
    playerActivityData: PlayerActivityData,
    activityEntity: ActivityEntity,
    resource: ActivityResource
  ) {

  }

  override fun balance(self: PlayerManager.Self, activityData: PlayerActivityData) {
  }

  override fun onTaskEvent(
    self: PlayerManager.Self,
    event: PlayerTaskEventLike,
    playerActivityData: PlayerActivityData,
    activityEntity: ActivityEntity,
    resource: ActivityResource
  ) {
    if (!TaskActivityResourceSet.extension().containsTargetType(event.taskEvent.targetType)) {
      return
    }
    TaskActivityTaskService(playerActivityData, targetHandlerProvider, rewardService)
      .onEvent(self, event.taskEvent)
  }

  override fun getDataProto(self: PlayerManager.Self, playerActivityData: PlayerActivityData): Message<*, *> {
    return TaskActivityDataProto()
  }
}
