package pig
package utils.db

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import utils.base.config.enums.DbConfig.*

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import sqala.jdbc.JdbcContext
// MySQL
import sqala.metadata.MysqlDialect
// PostgreSQL
// import sqala.metadata.PostgresqlDialect

lazy val db: JdbcContext = JdbcContext(dataSource(), MysqlDialect, true)

// 初始化数据库
def migrate(url: String = url, username: String = username, password: String = password): Unit = if migrateOnStart then {
  // 初始化 Flyway
  val flyway = Flyway
    .configure()
    .dataSource(url, username, password)
    // 当对一个非空的数据库执行 migrate() 时，自动对其进行基线化处理：
    // 1. 创建 flyway_schema_history 表（如果不存在）
    // 2. 插入一条记录，标记当前数据库已处于某个版本（默认版本 1）
    .baselineOnMigrate(true)
    // 设置基线版本从 0 开始，这样才能执行 V1
    .baselineVersion("0")
    // 版本控制日志表，默认 flyway_schema_history，不同系统建议修改数据
    .table("flyway_schema_history")
    .load()

  // flyway.migrate()：自动执行所有未执行过的迁移
  flyway.migrate()
}

private def dataSource(
  url: String = url,
  username: String = username,
  password: String = password
): HikariDataSource = {
  // 设置 Hikari 连接池日志等级为 ERROR，避免打印噪音
  LoggerFactory
    .getLogger("com.zaxxer.hikari")
    .asInstanceOf[ch.qos.logback.classic.Logger]
    .setLevel(ch.qos.logback.classic.Level.ERROR)
  val hc = HikariConfig()
  hc.setJdbcUrl(url)
  hc.setUsername(username)
  hc.setPassword(password)
  hc.setMaximumPoolSize(maximumPoolSize)
  hc.setMinimumIdle(minimumIdle)
  hc.setIdleTimeout(idleTimeout)
  hc.setMaxLifetime(maxLifetime)
  hc.setThreadFactory(Thread.ofVirtual().factory())
  HikariDataSource(hc)
}

extension (s: String) {
  // 有 SQL 注入风险，若是使用应该传入纯 SQL，不推荐直接使用字符串插值传递参数
  def sql = sqala.dynamic.native.NativeSql(s, Array())

  // 表名可以通过后端字符串插值插入，但是前后端传递的参数不要直接使用字符串插值，以下方法
  // 通过字符串中的 ? 补充参数，例如 s"SELECT id, name FROM $table WHERE id = ?".sql(1L)，
  // 假设 table 变量在后端已固定
  def sql(arg: Any*) = sqala.dynamic.native.NativeSql(s, arg.toArray)
}
