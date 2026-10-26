package pig
package utils.async

import utils.base.config.enums.AppConfig

import org.slf4j.MDC

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait Ec {
  given ExecutionContext = MdcExecutionContext(ec)
}

private class MdcExecutionContext(delegate: ExecutionContext) extends ExecutionContextExecutor {
  override def execute(runnable: Runnable): Unit = {
    // 提交任务时捕获 MDC Map，而不是在线程创建时捕获
    val mdcContext = MDC.getCopyOfContextMap
    delegate.execute(() => {
      if mdcContext != null then MDC.setContextMap(mdcContext)
      try runnable.run()
      finally MDC.clear()
    })
  }

  override def reportFailure(cause: Throwable): Unit = delegate.reportFailure(cause)
}

private val counter = new AtomicInteger(0)

// 将固定大小线程池转为 Scala 的执行上下文，这里没有使用虚拟线程；
// 此外若是使用虚拟线程则不要池化，虚拟线程可以通过信号量限制操作数
private val ec = ExecutionContext.fromExecutor(
  Executors.newFixedThreadPool(AppConfig.poolSize, r => {
    val t = new Thread(r, s"${AppConfig.name}-pool-${counter.incrementAndGet()}")
    t.setDaemon(true)
    t
  })
)
