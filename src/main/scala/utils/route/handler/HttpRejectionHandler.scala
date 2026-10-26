package pig
package utils.route.handler

import io.circe.syntax.*
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives.{complete, extractRequest}
import org.apache.pekko.http.scaladsl.server.RejectionHandler
import utils.result.{Result, StatusCode}

object HttpRejectionHandler {
  // 提供隐式的请求拒绝处理器
  given RejectionHandler = RejectionHandler
    .newBuilder()
    .handleNotFound {
      complete(
        StatusCodes.OK,
        HttpEntity(
          ContentTypes.`application/json`,
          Result.status(StatusCode.NotFound).asJson.toString
        )
      )
    }
    .result()
}
