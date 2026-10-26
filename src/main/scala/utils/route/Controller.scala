package pig
package utils.route

import org.slf4j.LoggerFactory
import sqala.jdbc.Logger
import sttp.tapir.Tapir
import sttp.tapir.json.circe.TapirJsonCirce
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.Future

// 控制层特质
trait Controller extends Tapir, TapirJsonCirce {
  // 暴露服务端点
  val serverEndpointList: List[ServerEndpoint[Any, Future]]
}