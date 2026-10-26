package pig
package utils.result

import utils.base.ColoredLog.e
import utils.result.ErrorResult.printMessage

import com.typesafe.scalalogging.LazyLogging

trait ErrorResult(val code: Int, val message: String) {
  printMessage(code, message)
}

object ErrorResult extends LazyLogging {
  private def printMessage(code: Int, message: String): Unit = logger.error(s"code: $code，message：$message".e)
}
