package play.example.game.app.module.player

import org.springframework.stereotype.Component
import play.example.game.app.module.mail.MailService
import play.example.game.app.module.mail.entity.Mail
import play.example.game.app.module.player.PlayerManager.*
import play.example.game.app.module.player.condition.PlayerCondition
import play.example.game.app.module.player.condition.PlayerConditionService
import play.example.game.app.module.player.event.PlayerEvent
import play.example.game.app.module.player.event.PlayerEventBus
import play.example.game.app.module.player.event.subscribe
import play.example.game.app.module.playertask.event.IPlayerTaskEvent
import play.example.game.app.module.reward.RewardService
import play.example.game.app.module.reward.model.*
import play.util.control.Result2

@Component
class PlayerServiceFacade(
  val playerService: PlayerService,
  val onlinePlayerService: OnlinePlayerService,
  val playerConditionService: PlayerConditionService,
  val rewardService: RewardService,
  val eventBus: PlayerEventBus,
  val mailService: MailService
) {

  fun todayHasLogin(id: Long): Boolean {
    return playerService.todayHasLogin(id)
  }

  fun isPlayerExists(playerId: Long) = playerService.isPlayerExists(playerId)

  fun isPlayerOnline(playerId: Long) = onlinePlayerService.isOnline(playerId)

  fun getPlayerNameOrNull(playerId: Long): String? = playerService.getPlayerNameOrNull(playerId)

  fun getPlayerNameOrEmpty(playerId: Long): String? = playerService.getPlayerNameOrElse(playerId, "")

  fun checkConditions(self: Self, conditions: Collection<PlayerCondition>): Result2<Nothing> {
    return playerConditionService.check(self, conditions)
  }

  fun tryAndExecReward(
    self: Self,
    rewardList: RewardList,
    logSource: Int,
    bagFullStrategy: BagFullStrategy = BagFullStrategy.Mail,
    checkFcm: Boolean = true
  ): Result2<RewardResultSet> {
    return rewardService.tryAndExecReward(self, rewardList, logSource, bagFullStrategy, checkFcm)
  }

  fun tryAndExecCost(self: Self, costList: CostList, logSource: Int): Result2<CostResultSet> {
    return rewardService.tryAndExecCost(self, costList, logSource)
  }

  fun sendMail(self: Self, mail: Mail) {
    mailService.sendMail(self, mail)
  }

  fun sendMail(playerId: Long, mail: Mail) {
    mailService.sendMail(playerId, mail)
  }

  fun publishEvent(event: PlayerEvent) {
    eventBus.publish(event)
  }

  fun publishTaskEvent(playerId: Long, event: IPlayerTaskEvent) {
    eventBus.publish(playerId, event)
  }

  inline fun <reified T> subscribeEvent(noinline action: (Self, T) -> Unit) {
    eventBus.subscribe(action)
  }

  inline fun <reified T> subscribeEvent(noinline action: (Self) -> Unit) {
    eventBus.subscribe<T>(action)
  }
}
