package play.example.game.app.module.activity.impl.login

import com.squareup.wire.Message
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples
import org.springframework.stereotype.Component
import play.example.common.StatusCode
import play.example.game.app.module.activity.base.ActivityHandler
import play.example.game.app.module.activity.base.ActivityType
import play.example.game.app.module.activity.base.PlayerActivityDataKey
import play.example.game.app.module.activity.base.PlayerActivityService
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.entity.PlayerActivityData
import play.example.game.app.module.activity.base.res.ActivityResource
import play.example.game.app.module.activity.base.res.ActivityTypeResourceSet
import play.example.game.app.module.activity.impl.login.domain.LoginActivityLogSource
import play.example.game.app.module.activity.impl.login.domain.LoginActivityStatusCode
import play.example.game.app.module.activity.impl.login.res.LoginActivityResourceSet
import play.example.game.app.module.mail.entity.Mail
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.PlayerServiceFacade
import play.example.game.app.module.player.event.PlayerNewDayStartEvent
import play.example.game.app.module.player.event.subscribe
import play.example.game.app.module.reward.message.toProto
import play.example.game.app.module.reward.model.RewardList.Companion.collect
import play.example.module.activity.message.LoginActivityDataProto
import play.example.reward.message.RewardResultSetProto
import play.util.control.Result2
import play.util.control.map
import play.util.primitive.Bit

/**
 *
 * @author LiangZengle
 */
@Component
class LoginActivityHandler(
  private val serviceFacade: PlayerServiceFacade,
  private val playerActivityService: PlayerActivityService
) : ActivityHandler {

  init {
    serviceFacade.subscribeEvent<PlayerNewDayStartEvent>(::increaseLoginDays)
  }

  override fun type(): ActivityType = ActivityType.LOGIN

  private fun increaseLoginDays(self: PlayerManager.Self) {
    playerActivityService.process(self, ActivityType.LOGIN) { _, playerActivityData ->
      val data = PlayerActivityDataKey.Login.getOrCreate(playerActivityData)
      data.days++
      StatusCode.Success
    }
  }

  fun getReward(self: PlayerManager.Self, activityId: Int, day: Int): Result2<RewardResultSetProto> {
    val resource = LoginActivityResourceSet.getByKeyOrNull(PrimitiveTuples.pair(activityId, day)) ?: return StatusCode.ResourceNotFound
    return playerActivityService.process(self, activityId) { _, playerActivityData ->
      val data = PlayerActivityDataKey.Login.getOrCreate(playerActivityData)
      if (resource.day < data.days) {
        LoginActivityStatusCode.LoginNotEnough
      } else if (Bit.is1(data.rewardedDays, day)) {
        LoginActivityStatusCode.RewardReceived
      } else {
        serviceFacade.tryAndExecReward(self, resource.rewards, LoginActivityLogSource.Reward).map {
          data.setRewarded(day)
          it.toProto()
        }
      }
    }
  }

  override fun join(
    self: PlayerManager.Self,
    playerActivityData: PlayerActivityData,
    activityEntity: ActivityEntity,
    resource: ActivityResource
  ) {
    if (serviceFacade.todayHasLogin(self.id)) {
      val data = PlayerActivityDataKey.Login.getOrCreate(playerActivityData)
      data.days++
    }
  }

  override fun balance(self: PlayerManager.Self, activityData: PlayerActivityData) {
    val data = PlayerActivityDataKey.Login.getOrCreate(activityData)
    val rewardList = LoginActivityResourceSet.list().asSequence()
      .filter { it.activityId == activityData.activityId && it.day <= data.days && !data.isRewarded(it.day) }
      .map { it.rewards }
      .collect()
    if (rewardList.isEmpty()) {
      return
    }
    val mail = Mail {
      titleAndContent(ActivityTypeResourceSet.getOrNull(type().id), { it.mailTitleId }, { it.mailContentId })
      rewards(rewardList, LoginActivityLogSource.Reward)
    }
    serviceFacade.sendMail(self, mail)
  }

  override fun getDataProto(self: PlayerManager.Self, playerActivityData: PlayerActivityData): Message<*, *> {
    val data = PlayerActivityDataKey.Login.getOrCreate(playerActivityData)
    return LoginActivityDataProto(data.days, data.rewardedDays)
  }
}
