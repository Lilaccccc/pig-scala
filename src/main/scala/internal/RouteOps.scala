package pig
package internal

import utils.base.ColoredLog.*
import utils.result.{ErrorResult, Result}
import utils.route.HttpService.ec

import com.typesafe.scalalogging.LazyLogging
import sttp.tapir.PublicEndpoint
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.Future
import scala.util.{Failure, Success}

object RouteOps extends LazyLogging:
  // 保留不需要包装返回体的方法，用于提供给 OpenAPI 的 doc.yaml 配置文件的路由使用
  extension [I, O](endpoint: PublicEndpoint[I, Unit, O, Any]) {
    def logic(logic: I => O): ServerEndpoint[Any, Future] = endpoint.serverLogicSuccess(input => Future(logic(input)))
  }

  // 统一返回结构体包装，阻塞
  extension [I, O](endpoint: PublicEndpoint[I, Unit, Result[O], Any]) {
    def logicWithResult(logic: I => O): ServerEndpoint[Any, Future] = endpoint.serverLogicSuccess { input =>
      val func = () => try {
        val resp = logic(input)
        resp match
          case e: ErrorResult => Result.errorWithCode(e.code, e.message)
          case _              => Result(resp)
      } catch case e =>
          logger.error(e.getMessage.e)
          Result.error()

      Future(func())
    }

    // 统一返回结构体包装，异步
    def logicWithResultAsync(logic: I => Future[O]): ServerEndpoint[Any, Future] = endpoint.serverLogicSuccess { input =>
      logic(input).transform {
        case Success(resp) =>
          resp match
            case e: ErrorResult => Success(Result.errorWithCode(e.code, e.message))
            case _              => Success(Result(resp))
        case Failure(e) =>
          logger.error(e.getMessage.e)
          Success(Result.error())
      }
    }

    // 统一返回结构体包装，异步 + Either
    def logicWithEitherAsync(logic: I => Future[Either[ErrorResult, O]]): ServerEndpoint[Any, Future] = endpoint.serverLogicSuccess { input =>
      logic(input).transform {
        case Success(either) =>
          either match
            case Left(e)  => Success(Result.errorWithCode(e.code, e.message))
            case Right(o) => Success(Result(o))
        case Failure(e) =>
          logger.error(e.getMessage.e)
          Success(Result.error())
      }
    }
  }
