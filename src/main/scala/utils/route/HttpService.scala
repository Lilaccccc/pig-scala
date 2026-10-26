package pig
package utils.route

import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.Done
import org.apache.pekko.actor.{ActorSystem, CoordinatedShutdown}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Route
import utils.base.ColoredLog.*
import utils.base.config.enums.AppConfig
import utils.route.handler.HttpRejectionHandler.given_RejectionHandler

import scala.concurrent.ExecutionContextExecutor

object HttpService extends LazyLogging {
  given system: ActorSystem          = ActorSystem(AppConfig.name)
  given ec: ExecutionContextExecutor = system.dispatcher

  def init(route: Route): Unit = Http()
    .newServerAt("0.0.0.0", AppConfig.port)
    // seal 方法会应用隐式提供的 RejectionHandler，并自动处理所有未匹配的路径
    .bind(Route.seal(route))
    .foreach { binding =>
      this.logger.info(s"服务器启动成功: ${binding.localAddress}".i)

      // 配置优雅关闭
      CoordinatedShutdown(system).addTask(
        CoordinatedShutdown.PhaseServiceUnbind,
        "unbind-http-server"
      ) { () =>
        this.logger.info("开始关闭 HTTP 服务...".i)
        binding.unbind().map { _ =>
          this.logger.info("HTTP 服务已关闭".i)
          Done
        }
      }
    }
}

