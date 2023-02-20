package play.example.game.app.module.activity.impl.login.res

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import org.eclipse.collections.api.tuple.primitive.IntIntPair
import org.eclipse.collections.impl.tuple.primitive.IntIntPairImpl
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples
import play.example.game.app.module.activity.base.res.ActivityResource
import play.example.game.app.module.reward.model.RewardList
import play.res.AbstractResource
import play.res.ResourceSetProvider
import play.res.UniqueKey
import play.res.validation.constraints.ReferTo

/**
 *
 * @author LiangZengle
 */
class LoginActivityResource : AbstractResource(), UniqueKey<IntIntPair> {

  override val id: Int = 0

  @ReferTo(ActivityResource::class)
  val activityId: Int = 0

  @Max(63)
  val day = 0

  @Valid
  val rewards = RewardList.Empty

  override fun key(): IntIntPair {
    return PrimitiveTuples.pair(activityId, day)
  }

  override fun initialize(resourceSetProvider: ResourceSetProvider, errors: MutableCollection<String>) {
    super.initialize(resourceSetProvider, errors)
  }
}
