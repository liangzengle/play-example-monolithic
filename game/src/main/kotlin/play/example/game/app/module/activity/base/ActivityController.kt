package play.example.game.app.module.activity.base

import org.springframework.stereotype.Component
import play.example.game.app.module.ModuleId
import play.example.game.app.module.player.PlayerManager
import play.example.module.activity.message.ActivityProto
import play.mvc.*

/**
 *
 * @author LiangZengle
 */
@Component
@Controller(ModuleId.Activity)
class ActivityController(private val playerActivityService: PlayerActivityService) : AbstractController(ModuleId.Activity) {

  @Cmd(1)
  fun list(self: PlayerManager.Self) = RequestResult.ok {
    playerActivityService.listActivities(self)
  }

  /**
   * 推送活动开始
   */
  @Cmd(101)
  lateinit var activityStart: Push<ActivityProto>

  /**
   * 推送活动隐藏
   */
  @Cmd(102)
  lateinit var activityHide: Push<Int>
}
