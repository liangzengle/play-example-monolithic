package play.example.game.app.module.activity.impl.login.data

import play.util.primitive.Bit

/**
 *
 * @author LiangZengle
 */
class LoginActivityData {
  var days = 0
  var rewardedDays = 0L

  fun setRewarded(day: Int) {
    rewardedDays = Bit.set1(rewardedDays, day)
  }

  fun isRewarded(day: Int) = Bit.is1(rewardedDays, day)
}
