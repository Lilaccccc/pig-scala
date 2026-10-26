package pig
package utils.base.config.enums

import utils.base.config
import utils.base.config.Config

enum RedisConfig(val field: String) extends Config("redis") {
  private case Host                        extends RedisConfig("host")
  private case Port                        extends RedisConfig("port")
  private case Password                    extends RedisConfig("password")
  private case KeyPrefix                   extends RedisConfig("keyPrefix")
  private case Database                    extends RedisConfig("database")
  private case MaxTotal                    extends RedisConfig("maxTotal")
  private case MaxIdle                     extends RedisConfig("maxIdle")
  private case MinIdle                     extends RedisConfig("minIdle")
  private case EvictorShutdownTimeout      extends RedisConfig("evictorShutdownTimeout")
  private case TestOnBorrow                extends RedisConfig("testOnBorrow")
  private case TestOnReturn                extends RedisConfig("testOnReturn")
  private case TestWhileIdle               extends RedisConfig("testWhileIdle")
  private case TimeoutMillis               extends RedisConfig("timeoutMillis")
  private case ConnectionTimeoutMillis     extends RedisConfig("connectionTimeoutMillis")
  private case SocketTimeoutMillis         extends RedisConfig("socketTimeoutMillis")
  private case BlockingSocketTimeoutMillis extends RedisConfig("blockingSocketTimeoutMillis")
  private case ClearnOnStart               extends RedisConfig("clearnOnStart")
}

object RedisConfig {
  def maxTotal: Int                    = config.get[Int](MaxTotal)
  def maxIdle: Int                     = config.get[Int](MaxIdle)
  def minIdle: Int                     = config.get[Int](MinIdle)
  def evictorShutdownTimeout: Int      = config.get[Int](EvictorShutdownTimeout)
  def testOnBorrow: Boolean            = config.get[Boolean](TestOnBorrow)
  def testOnReturn: Boolean            = config.get[Boolean](TestOnReturn)
  def testWhileIdle: Boolean           = config.get[Boolean](TestWhileIdle)
  def host: String                     = config.get[String](Host)
  def port: Int                        = config.get[Int](Port)
  def password: String                 = config.get[String](Password)
  def database: Int                    = config.get[Int](Database)
  def timeoutMillis: Int               = config.get[Int](TimeoutMillis)
  def connectionTimeoutMillis: Int     = config.get[Int](ConnectionTimeoutMillis)
  def socketTimeoutMillis: Int         = config.get[Int](SocketTimeoutMillis)
  def blockingSocketTimeoutMillis: Int = config.get[Int](BlockingSocketTimeoutMillis)
  def clearnOnStart: Boolean           = config.get[Boolean](ClearnOnStart)
  def keyPrefix: String                = config.get[String](KeyPrefix)
}
