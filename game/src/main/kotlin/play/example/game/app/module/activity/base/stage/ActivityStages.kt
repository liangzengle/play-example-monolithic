package play.example.game.app.module.activity.base.stage

/**
 *
 * @author LiangZengle
 */
@JvmInline
value class ActivityStages(val value: Int) {
  fun contains(stage: ActivityStage) = (stage.identifier and value) != 0

  fun contains(stageIdentifier: Int) = (stageIdentifier and value) != 0

  infix fun or(stage: ActivityStage): ActivityStages {
    return ActivityStages(value or stage.identifier)
  }

  infix fun or(stages: ActivityStages): ActivityStages {
    return ActivityStages(value or stages.value)
  }

  infix fun or(stages: Int): ActivityStages {
    return ActivityStages(value or stages)
  }

  companion object {
    @JvmStatic
    val ANY: ActivityStages = ActivityStages(ActivityStage.VALUES.fold(0) { r, v -> r or v.identifier })

    @JvmStatic
    val START = ActivityStage.Start.toStages()

    @JvmStatic
    val END = ActivityStage.End.toStages()

    @JvmStatic
    val CLOSE = ActivityStage.Close.toStages()

    @JvmStatic
    fun not(stage: ActivityStage): ActivityStages = ActivityStages(ANY.value and stage.identifier.inv())

    @JvmStatic
    fun not(stages: ActivityStages): ActivityStages = ActivityStages(ANY.value and stages.value.inv())
  }
}

