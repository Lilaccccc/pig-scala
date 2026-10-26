package pig
package internal

import utils.base.ColoredLog.*
import utils.redis.RedisOps
import utils.route.{Controller, HttpService, RouteUtil, initControllers}

import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.http.cors.scaladsl.CorsDirectives.cors
import utils.db.migrate

object InitService extends LazyLogging {
  def apply(): Unit = {
    migrate()
    RedisOps.init()
    startHttp()
  }

  private def startHttp(): Unit = {
    val controller = initControllers[Controller]
    logger.debug(s"初始化路由数量：${controller.size}".d)
    val endpointList = controller.flatMap(_.serverEndpointList)
    logger.debug(s"初始化端点：${endpointList.size} 个，${endpointList.map(ep => s"${ep.endpoint.showShort}").mkString("[", ", ", "]")}".d)
    HttpService.init(cors()(RouteUtil.toRoute(endpointList)))
  }
}
