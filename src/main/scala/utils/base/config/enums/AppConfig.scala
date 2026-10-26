package pig
package utils.base.config.enums

import utils.base.config
import utils.base.config.Config

enum AppConfig(val field: String) extends Config("server") {
  private case Name extends AppConfig("name")
  private case Port extends AppConfig("port")
  private case PoolSize extends AppConfig("poolSize")
}

object AppConfig {
  def name: String = config.get[String](Name)
  def port: Int = config.get[Int](Port)
  def poolSize: Int = config.get[Int](PoolSize)
}