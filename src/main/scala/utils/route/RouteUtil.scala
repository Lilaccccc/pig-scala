package pig
package utils.route

import utils.route.HttpService.ec
import utils.route.handler.{CodeFailureHandler, TraceIdInterceptor}

import org.apache.pekko.http.scaladsl.server.Route
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.pekkohttp.{PekkoHttpServerInterpreter, PekkoHttpServerOptions}

import scala.concurrent.Future

object RouteUtil {
  // 配置 ServerOptions
  private lazy val serverOptions: PekkoHttpServerOptions = PekkoHttpServerOptions.customiseInterceptors
    // 设置解码异常处理器
    .decodeFailureHandler(CodeFailureHandler.apply)
    .addInterceptor(TraceIdInterceptor.requestInterceptor)
    .addInterceptor(TraceIdInterceptor.responseInterceptor)
    .options

  // 配置全局 PekkoHttpServerInterpreter 实例
  private lazy val pekko = PekkoHttpServerInterpreter(serverOptions)

  // 组合路由，此处传递的是 ServerEndpoint 服务端点列表，而不是 AnyEndpoint 或 PublicEndpoint 端点了列表
  def toRoute(endpoints: List[ServerEndpoint[Any, Future]]): Route = pekko.toRoute(endpoints)
}
