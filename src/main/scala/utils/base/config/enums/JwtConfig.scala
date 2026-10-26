package pig
package utils.base.config.enums

import utils.base.config
import utils.base.config.Config

enum JwtConfig(val field: String) extends Config("jwt") {
  private case Secret     extends JwtConfig("secret")
  private case AccessTtl  extends JwtConfig("accessTtl")
  private case RefreshTtl extends JwtConfig("refreshTtl")
}

object JwtConfig {
  def secret: String  = config.get[String](Secret).trim
  def accessTtl: Int  = config.get[Int](AccessTtl)
  def refreshTtl: Int = config.get[Int](RefreshTtl)
}