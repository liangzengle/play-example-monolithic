package play.example.game.app.module.activity.base

import org.springframework.stereotype.Component
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.entity.ActivityEntityCache
import play.example.game.app.module.activity.base.res.ActivityResource
import play.example.game.app.module.activity.base.res.ActivityResourceSet
import play.example.game.app.module.activity.base.stage.ActivityStage
import play.example.game.app.module.activity.base.stage.ActivityStages
import play.util.collection.ConcurrentLongLongMap
import play.util.exists

/**
 *
 * @author LiangZengle
 */
@Component
class ActivityCache(private val entityCache: ActivityEntityCache) {

  private val activityStageCache = ConcurrentLongLongMap()

  fun update(activityId: Int, stage: ActivityStage) {
    activityStageCache.put(activityId.toLong(), stage.identifier.toLong())
  }

  fun getActivities(activityType: ActivityType, requireStages: ActivityStages): Sequence<ActivityEntity> {
    return activityStageCache.entries().asSequence()
      .filter { requireStages.contains(it.value.toInt()) }
      .filter { ActivityResourceSet.get(it.key.toInt()).exists { res -> res.type == activityType } }
      .map { e -> entityCache.getOrThrow(e.key.toInt()) }
  }


  fun getActivities(requireStages: ActivityStages): Sequence<ActivityEntity> {
    return activityStageCache.entries().asSequence()
      .filter { requireStages.contains(it.value.toInt()) }
      .map { e -> entityCache.getOrThrow(e.key.toInt()) }
  }

  fun getActivity(activityId: Int, requireStages: ActivityStages): ActivityEntity? {
    if (!activityStageCache.containsKey(activityId.toLong())) {
      return null
    }
    val entity = entityCache.getOrNull(activityId) ?: return null
    if (!requireStages.contains(entity.stage)) {
      return null
    }
    return entity
  }
}
