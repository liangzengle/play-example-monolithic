package play.example.game.app.module.activity.base.stage

/**
 *
 * @author LiangZengle
 */
enum class ActivityStage(val handler: ActivityStageHandler) {
  None(NoneStageHandler),
  Init(InitStageHandler),
  Start(StartStageHandler),
  End(EndStageHandler),
  Close(CloseStageHandler);

  companion object {
    @JvmStatic
    val VALUES = values()
  }

  val identifier = 1 shl (ordinal)

  fun prev(): ActivityStage? = if (ordinal in 1..<VALUES.size) VALUES[ordinal - 1] else null

  fun next(): ActivityStage? = if (ordinal in 0..<VALUES.size - 1) VALUES[ordinal + 1] else null

  fun toStages() = ActivityStages(identifier)
}
