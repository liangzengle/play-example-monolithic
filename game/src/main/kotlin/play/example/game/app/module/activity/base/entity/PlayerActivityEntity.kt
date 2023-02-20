package play.example.game.app.module.activity.base.entity

import org.eclipse.collections.api.factory.primitive.IntObjectMaps
import play.example.game.app.module.player.entity.AbstractPlayerEntity
import play.util.collection.SerializableAttributeMap
import java.util.*

/**
 *
 * @author LiangZengle
 */
class PlayerActivityEntity(id: Long) : AbstractPlayerEntity(id) {

  private val dataMap = IntObjectMaps.mutable.empty<PlayerActivityData>()

  fun getOrCreate(activityId: Int) = dataMap.getIfAbsentPutWithKey(activityId, ::PlayerActivityData)

  fun get(activityId: Int): PlayerActivityData? = dataMap.get(activityId)

  fun listData(): Collection<PlayerActivityData> = Collections.unmodifiableCollection(dataMap.values())
}

class PlayerActivityData(val activityId: Int) {

  companion object {
    const val JOINED = 1
    const val HIDDEN = 2
  }

  /** 活动版本: [ActivityEntity.openTimes] */
  var version = 0

  /** 活动状态 */
  private var state = 0

  /** 是否已结算 */
  var balanced = false

  /** 活动数据 */
  var data = SerializableAttributeMap()
    private set

  fun join(version: Int) {
    this.version = version
    state = JOINED
    balanced = false
    data = SerializableAttributeMap()
  }

  fun isJoined() = state == JOINED

  fun hide() {
    state = HIDDEN
  }

  fun isHidden() = state == HIDDEN

  fun isEmptyState() = state == 0

  override fun toString(): String {
    return "PlayerActivityData(activityId=$activityId, version=$version, state=$state, balanced=$balanced)"
  }
}
