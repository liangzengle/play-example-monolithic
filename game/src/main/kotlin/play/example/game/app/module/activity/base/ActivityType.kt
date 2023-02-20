package play.example.game.app.module.activity.base

import play.codegen.EnumId
import play.util.enumeration.IdEnum

/**
 *
 * @author LiangZengle
 */
enum class ActivityType(@JvmField @field:EnumId val id: Int) : IdEnum {
  LOGIN(1),
  TASK(2),
  PAY(3);

  override fun id(): Int {
    return id
  }
}
