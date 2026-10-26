package pig
package utils.health

import oshi.SystemInfo
import oshi.hardware.{CentralProcessor, GlobalMemory}
import oshi.software.os.{FileSystem, OSFileStore, OperatingSystem}
import oshi.util.{FormatUtil, Util}
import utils.base.Converters.*

object SystemHealth {
  // 初始化 OSHI
  private val si = SystemInfo()
  // 获取硬件抽象层
  private val har       = si.getHardware
  private val processor = har.getProcessor
  private val memory    = har.getMemory
  private val os        = si.getOperatingSystem

  def main(args: Array[String]): Unit = {
    println(cpu)
    println(cache)
    println(diskUsage)
    println(uptime)
    println(load)
  }

  // CPU 使用率 (计算两次采样之间的负载)
  def cpu: String = {
    val prevTicks: Array[Long] = processor.getSystemCpuLoadTicks
    Util.sleep(1000) // 间隔 1 秒
    f"${processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100}%.2f"
  }

  // 内存使用率
  def cache: String = {
    val totalMemory     = memory.getTotal
    val availableMemory = memory.getAvailable
    f"${(totalMemory - availableMemory) * 100.0 / totalMemory}%.2f"
  }

  // 磁盘使用率 (以根目录 "/" 或 "C:\" 为例)
  def diskUsage: String = {
    val fileSystem = os.getFileSystem
    val fileStores = fileSystem.getFileStores.toScala
    val disk       = fileStores.find { fs =>
      fs.getMount.equals("/") ||
      fs.getMount.equals("C:\\")
    }.map(fs => {
      val totalSpace  = fs.getTotalSpace
      val usableSpace = fs.getUsableSpace
      ((totalSpace - usableSpace) * 100 / totalSpace).toDouble
    }).getOrElse(Double.NaN)
    f"$disk%.2f"
  }

  // 系统运行时间
  def uptime: String = {
    // 输出示例：5 days, 08:53:36
    // FormatUtil.formatElapsedSecs(os.getSystemUptime)
    val uptimeSeconds = os.getSystemUptime
    val days          = uptimeSeconds / 86400
    val hours         = (uptimeSeconds % 86400) / 3600
    val minutes       = (uptimeSeconds % 3600) / 60

    if days > 0 then s"${days}d ${hours}h ${minutes}m"
    else if hours > 0 then s"${hours}h ${minutes}m"
    else s"${minutes}m"
  }

  // 获取系统负载
  // 3 表示获取 1、5、15 分钟的平均值，Window 系统会显示 -1.00 -1.00 -1.00（不适用）
  def load: String = {
    val loadAverage: Array[Double] = processor.getSystemLoadAverage(3)
    loadAverage.map(i => f"$i%.2f").mkString(" ")
  }
}
