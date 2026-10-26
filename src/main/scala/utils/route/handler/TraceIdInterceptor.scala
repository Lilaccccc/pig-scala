package pig
package utils.route.handler

import org.slf4j.MDC
import sttp.model.Header
import sttp.tapir.AttributeKey
import sttp.tapir.model.ServerRequest
import sttp.tapir.server.interceptor.{RequestInterceptor, RequestResult}
import utils.base.idgenerator.IdGenerator

import scala.concurrent.Future

object TraceIdInterceptor {

  private val traceIdKey = new AttributeKey[String]("X-Trace-Id")

  def requestInterceptor: RequestInterceptor[Future] = RequestInterceptor.transformServerRequest { request =>
    val traceId = request.headers
      .find(_.name.equalsIgnoreCase("X-Trace-Id"))
      .map(_.value)
      .getOrElse(IdGenerator.snowId.toString)

    MDC.put("traceId", traceId)
    Future.successful(request.attribute(traceIdKey, traceId))
  }

  def responseInterceptor: RequestInterceptor[Future] = {
    val transform = new RequestInterceptor.RequestResultTransform[Future] {
      override def apply[B](request: ServerRequest, result: RequestResult[B]): Future[RequestResult[B]] = {
        val traceId           = request.attribute(traceIdKey).getOrElse("")
        val transformedResult = result match
          case RequestResult.Response(response, source) =>
            val traceIdHeader      = Header(traceIdKey.typeName, traceId)
            val headersWithTraceId = response.headers :+ traceIdHeader
            RequestResult.Response(response.copy(headers = headersWithTraceId), source)
          case other => other
        Future.successful(transformedResult)
      }
    }

    RequestInterceptor.transformResult(transform)
  }
}
