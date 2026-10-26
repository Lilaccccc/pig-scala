package pig
package utils.thread

import utils.base.idgenerator.IdGenerator
import org.slf4j.MDC

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

object Task {
  private lazy val backgroundExecutor = Executors.newCachedThreadPool
  private given ExecutionContext = ExecutionContext.fromExecutor(backgroundExecutor)

  def execute(task: Long => Unit): Future[Unit] = {
    val mdcContext = MDC.getCopyOfContextMap
    Future {
      try {
        if mdcContext != null then MDC.setContextMap(mdcContext)
        task(mdcContext.getOrDefault("traceId", IdGenerator.snowId.toString).toLong)
      } finally MDC.clear()
    }
  }
}
