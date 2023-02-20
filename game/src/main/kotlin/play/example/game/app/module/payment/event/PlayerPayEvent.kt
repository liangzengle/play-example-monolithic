package play.example.game.app.module.payment.event

import play.example.game.app.module.player.event.PlayerEvent

/**
 *
 * @author LiangZengle
 */
data class PlayerPayEvent(override val playerId: Long, val gold: Int, val money: Int) : PlayerEvent
