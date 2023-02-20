package play.example.game.app.module.activity.base

import org.eclipse.collections.api.factory.primitive.IntObjectMaps
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap
import play.example.game.app.module.activity.base.entity.PlayerActivityData
import play.example.game.app.module.activity.base.entity.PlayerActivityEntity
import play.example.game.app.module.activity.impl.login.data.LoginActivityData
import play.example.game.app.module.activity.impl.pay.data.PayActivityData
import play.example.game.app.module.task.entity.TaskData
import play.util.collection.SerializableAttributeKey
import play.util.collection.SerializableAttributeKey.Companion.valueOf

/**
 *
 * @author LiangZengle
 */
object PlayerActivityDataKey {

  @JvmStatic
  val Login = Def(valueOf("Login")) { LoginActivityData() }

  @JvmStatic
  val TaskData = Def(valueOf("TaskData")) { IntObjectMaps.mutable.empty<TaskData>() }

  @JvmStatic
  val Pay = Def(valueOf("Pay")) { PayActivityData() }


  class Def<T>(val key: SerializableAttributeKey<T>, private val initializer: () -> T) {

    fun getOrCreate(entity: PlayerActivityData): T {
      return entity.data.attr(key).computeIfAbsent(initializer)
    }

    fun getIfPresent(entity: PlayerActivityData): T? {
      return entity.data.attr(key).getValue()
    }
  }
}
