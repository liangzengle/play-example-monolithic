package play.example.game.app.module.reward

import com.google.common.base.Splitter
import com.google.common.collect.Collections2
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import org.eclipse.collections.impl.list.mutable.FastList
import play.example.game.app.module.reward.model.Cost
import play.example.game.app.module.reward.model.Reward
import play.example.game.app.module.reward.model.RewardList
import play.example.game.app.module.reward.res.RawReward
import play.util.collection.sizeCompareTo
import play.util.max

/**
 *
 * @author LiangZengle
 */
object RewardHelper {

  const val ElementSplitter = ';'
  const val AttributeSplitter = ','

  @JvmStatic
  fun mergeReward(rewards: Collection<Reward>): ImmutableList<Reward> {
    return merge(rewards, false, rewards.size)
  }

  @JvmStatic
  fun mergeReward(rewards: Sequence<Reward>): ImmutableList<Reward> {
    return merge(rewards, false)
  }

  @JvmStatic
  fun mergeCost(costs: Collection<Cost>): ImmutableList<Cost> {
    return ImmutableList.copyOf(
      Lists.transform(
        merge(Collections2.transform(costs) { it!!.reward }, true, costs.size),
        ::Cost
      )
    )
  }

  @JvmStatic
  fun mergeCost(costs: Sequence<Cost>): ImmutableList<Cost> {
    return ImmutableList.copyOf(Lists.transform(merge(costs.map { it.reward }, true), ::Cost))
  }


  @JvmStatic
  private fun merge(rewards: Sequence<Reward>, isCost: Boolean): ImmutableList<Reward> {
    return merge(rewards.asIterable(), isCost)
  }

  @JvmStatic
  private fun merge(rewards: Iterable<Reward>, isCost: Boolean, sizeHints: Int = -1): ImmutableList<Reward> {
    return when (if (sizeHints == -1) rewards.sizeCompareTo(1) else sizeHints.compareTo(1)) {
      -1 -> ImmutableList.of()
      0 -> {
        val reward = rewards.first()
        if (reward.num <= 0) {
          ImmutableList.of()
        } else {
          ImmutableList.of(reward)
        }
      }

      else -> {
        val merged = FastList<Reward>(sizeHints max 0)
        for (r in rewards) {
          if (r.num > 0) {
            val i = merged.indexOfFirst { it.canMerge(r, isCost) }
            if (i == -1) merged += r
            else merged[i] = merged[i] + r.num
          }
        }
        ImmutableList.copyOf(merged)
      }
    }
  }

  @JvmStatic
  fun merge(list1: RewardList, list2: RewardList): RewardList {
    if (list1.isEmpty()) {
      return list2
    }
    if (list2.isEmpty()) {
      return list1
    }
    val base: MutableList<Reward>
    val toBeMerged: List<Reward>
    if (list2.size() > list1.size()) {
      base = FastList(list2.asList())
      toBeMerged = list1.asList()
    } else {
      base = FastList(list1.asList())
      toBeMerged = list2.asList()
    }
    for (r in toBeMerged) {
      if (r.num > 0) {
        val i = base.indexOfFirst { it.canMerge(r, false) }
        if (i == -1) base += r
        else base[i] = base[i] + r.num
      }
    }
    return RewardList(ImmutableList.copyOf(base))
  }

  @JvmStatic
  fun parseRewardString(string: String): List<Reward> {
    return parseRewardString(string) { id, num -> Reward(id.toInt(), num.toLong()) }
  }

  @JvmStatic
  fun parseRewardStringAsRawRewards(string: String): List<RawReward> {
    return parseRewardString(string) { id, num -> RawReward(id.toInt(), num) }
  }

  @JvmStatic
  private fun <T : Any> parseRewardString(string: String, mapper: (String, String) -> T): List<T> {
    try {
      return Splitter.on(ElementSplitter).split(string)
        .asSequence()
        .filter { it.isNotBlank() }
        .map { element ->
          val iterator = Splitter.on(AttributeSplitter).split(element).iterator()
          val cfgId = iterator.next()
          val num = iterator.next()
          mapper(cfgId, num)
        }
        .toList()
    } catch (e: Exception) {
      throw IllegalArgumentException("奖励格式错误: $string", e)
    }
  }
}
