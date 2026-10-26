package pig
package utils.route.handler

import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.interceptor.decodefailure.DecodeFailureHandler
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.{DecodeResult, statusCode}
import utils.result.Result

import scala.concurrent.Future

// 自定义解码失败处理器
object CodeFailureHandler {
  def apply = DecodeFailureHandler.pure[Future] { ctx =>
    // 获取请求失败原因
    val errorMsg = ctx.failure match
      case DecodeResult.Mismatch(expected, actual) =>
        s"不支持的 HTTP 方法: $actual，期望: $expected"
      case DecodeResult.Error(_, e) =>
        s"JSON 请求体缺失或解析失败: ${e.getMessage}"
      case DecodeResult.InvalidValue(errors) =>
        s"参数值无效: ${errors.map(_.customMessage).mkString(", ")}"
      case DecodeResult.Missing          => "缺少必需的参数"
      case DecodeResult.Multiple(values) =>
        s"提供了多个值: ${values.mkString(", ")}"
      case null => "请求失败"

    Some(
      ValuedEndpointOutput(
        statusCode(StatusCode.Ok).and(jsonBody[Result[String]]),
        Result.error(errorMsg)
      )
    )
  }
}
