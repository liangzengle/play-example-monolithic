package play.example.game.app.module.activity.impl.pay

import com.squareup.wire.Message
import org.springframework.stereotype.Component
import play.example.common.StatusCode
import play.example.game.app.module.activity.base.ActivityHandler
import play.example.game.app.module.activity.base.ActivityType
import play.example.game.app.module.activity.base.PlayerActivityDataKey
import play.example.game.app.module.activity.base.PlayerActivityService
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.entity.PlayerActivityData
import play.example.game.app.module.activity.base.res.ActivityResource
import play.example.game.app.module.activity.base.stage.ActivityStages
import play.example.game.app.module.payment.event.PlayerPayEvent
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.event.PlayerEventBus
import play.example.game.app.module.player.event.subscribe
import play.example.module.activity.message.PayActivityDataProto

/**
 *
 * @author LiangZengle
 */
@Component
class PayActivityHandler(playerEventBus: PlayerEventBus, private val playerActivityService: PlayerActivityService) :
  ActivityHandler {

  init {
    playerEventBus.subscribe(::onPay)
  }

  override fun type(): ActivityType = ActivityType.PAY

  override fun join(
    self: PlayerManager.Self,
    playerActivityData: PlayerActivityData,
    activityEntity: ActivityEntity,
    resource: ActivityResource
  ) {
  }

  override fun balance(self: PlayerManager.Self, activityData: PlayerActivityData) {

  }

  private fun onPay(self: PlayerManager.Self, event: PlayerPayEvent) {
    playerActivityService.process(self, ActivityType.PAY, ActivityStages.START) { _, playerActivityData ->
      val data = PlayerActivityDataKey.Pay.getOrCreate(playerActivityData)
      data.goldTotal += event.gold
      StatusCode.Success
    }
  }

  override fun getDataProto(self: PlayerManager.Self, playerActivityData: PlayerActivityData): Message<*, *> {
    val data = PlayerActivityDataKey.Pay.getOrCreate(playerActivityData)
    return PayActivityDataProto(data.goldTotal)
  }
}
