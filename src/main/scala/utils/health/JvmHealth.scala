package pig
package utils.health

import com.sun.management.{OperatingSystemMXBean, ThreadMXBean}
import io.circe.Codec
import sttp.tapir.Schema
import utils.base.Converters.*

import java.lang.management.{ManagementFactory, MemoryUsage}
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId}

case class JvmStatus(
  heapUsed: Long,
  heapTotal: Long,
  threads: Int,
  maxThreads: Int,
  gcCount: Long,
  version: String,
  startTime: String
) derives Codec, Schema

object JvmHealth {
  private val rb = ManagementFactory.getRuntimeMXBean
  private val mb = ManagementFactory.getMemoryMXBean
  private val tb = ManagementFactory.getThreadMXBean.asInstanceOf[ThreadMXBean]
  private val gc = ManagementFactory.getGarbageCollectorMXBeans.toScala
  private val os = ManagementFactory.getOperatingSystemMXBean.asInstanceOf[OperatingSystemMXBean]

  def current: JvmStatus = {
    val version    = s"${rb.getVmName} ${rb.getSpecVersion}"
    val startTime  = formatTime(rb.getStartTime)
    val threads    = tb.getThreadCount
    val maxThreads = tb.getPeakThreadCount
    val hm         = mb.getHeapMemoryUsage
    val MB         = 1024 * 1024
    val heapUsed   = hm.getUsed / MB
    val heapTotal  = hm.getCommitted / MB

    JvmStatus(heapUsed, heapTotal, threads, maxThreads, 0L, version, startTime)
  }

  def formatTime(millis: Long): String = LocalDateTime
    .ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
}
