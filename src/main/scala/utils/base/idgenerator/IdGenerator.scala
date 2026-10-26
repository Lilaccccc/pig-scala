package pig
package utils.base.idgenerator

import java.math.BigInteger
import java.security.SecureRandom
import java.time.ZoneId

object IdGenerator {
  private lazy val snowGenerator = SnowflakeIdGenerator(0, 0)
  
  def snowId: Long = snowGenerator.generateUniqueId()

  def hex16Id(length: Int): String = {
    val bytes = new Array[Byte](length / 2)
    SecureRandom().nextBytes(bytes)
    BigInteger(1, bytes).toString(16)
  }

  def main(args: Array[String]): Unit = {
    println(hex16Id(22))

    (0 until 99999).foreach { _ =>
      val sId = snowId
      val parts = SnowflakeIdGenerator.parse(sId)
      val zdt = parts.instant.atZone(ZoneId.of("Asia/Shanghai"))
      // (生成 ID，生成该 ID 的时间, 该毫秒内的序号，格式化输出时间)
      println(s"${(sId, parts.instant, parts.sequence, zdt)}")
    }
  }
}

