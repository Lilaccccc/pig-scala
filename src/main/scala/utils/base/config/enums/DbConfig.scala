package pig
package utils.base.config.enums

import utils.base.config
import utils.base.config.Config

enum DbConfig(val field: String) extends Config("hikari") {
  private case Driver          extends DbConfig("driver")
  private case Url             extends DbConfig("jdbcUrl")
  private case Username        extends DbConfig("username")
  private case Password        extends DbConfig("password")
  private case MaximumPoolSize extends DbConfig("maximumPoolSize")
  private case MinimumIdle     extends DbConfig("minimumIdle")
  private case IdleTimeout     extends DbConfig("idleTimeout")
  private case MaxLifetime     extends DbConfig("maxLifetime")
  private case MigrateOnStart  extends DbConfig("migrateOnStart")
}

object DbConfig {
  def url: String             = config.get[String](Url)
  def username: String        = config.get[String](Username)
  def password: String        = config.get[String](Password)
  def maximumPoolSize: Int    = config.get[Int](MaximumPoolSize)
  def minimumIdle: Int        = config.get[Int](MinimumIdle)
  def idleTimeout: Int        = config.get[Int](IdleTimeout)
  def maxLifetime: Int        = config.get[Int](MaxLifetime)
  def migrateOnStart: Boolean = config.get[Boolean](MigrateOnStart)
}
