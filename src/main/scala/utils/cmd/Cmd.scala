package pig
package utils.cmd

import utils.base.ColoredLog.*

import com.typesafe.scalalogging.LazyLogging

import java.io.{BufferedReader, InputStreamReader}
import scala.util.Using

object Cmd extends LazyLogging {
  def executeBatFile(bat: String): Unit = try {
    val processBuilder = ProcessBuilder()
    processBuilder.command("cmd.exe", "/c", bat)
    processBuilder.redirectErrorStream(true)
    val process = processBuilder.start()
    val output  = StringBuilder()
    Using(BufferedReader(InputStreamReader(process.getInputStream))) { reader =>
      Iterator
        .continually(reader.readLine)
        .takeWhile(_ != null)
        .foreach(output.append)
    }
    val exitCode = process.waitFor()
    logger.debug(s"$bat 执行结果状态码：$exitCode".d)
  } catch {
    case e: Exception => logger.error(e.getMessage.e)
  }
}