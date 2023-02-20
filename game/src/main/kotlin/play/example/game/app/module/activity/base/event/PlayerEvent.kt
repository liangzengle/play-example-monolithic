package play.example.game.app.module.activity.base.event

import play.example.game.app.module.player.event.PlayerEvent

data class ActivityStartPlayerEvent(override val playerId: Long, val activityId: Int) : PlayerEvent

data class ActivityClosePlayerEvent(override val playerId: Long, val activityId: Int) : PlayerEvent
