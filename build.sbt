scalaVersion := "3.10.0"

lazy val root = rootProject
  .settings(
    name := "pig-scala",
    idePackagePrefix := Some("pig"),
    // 设置该项目的本地依赖包所在的目录，若不设置则默认为根目录的 `lib` 文件夹
    unmanagedBase := baseDirectory.value / "lib",
    // 使用 `sbt "show discoveredMainClasses"` 命令查看主类列表
    // 打包命令：sbt clean assembly
    assembly / mainClass := Some("pig.Application"),
    // 合并策略设置，当依赖冲突时需要配置，根据详细情况修改（问 AI..）
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "native-image", xs@_*) => MergeStrategy.first
      case PathList("META-INF", "versions", xs@_*) => MergeStrategy.first
      case PathList("META-INF", "services", xs @ _*)  => MergeStrategy.concat
      case PathList("META-INF", xs@_*) => MergeStrategy.discard
      case PathList("google", "protobuf", xs@_*) => MergeStrategy.first
      case PathList("smile", "plot", "vega", xs@_*) => MergeStrategy.first
      case PathList("module-info.class") => MergeStrategy.discard
      case x if x.endsWith("/module-info.class") => MergeStrategy.discard
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    },
  )

// 并行集合库
libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.2.0"
// 对象转换库
libraryDependencies += "io.scalaland" %% "chimney" % "2.1.0"
// Redis 依赖
libraryDependencies += "redis.clients" % "jedis" % "8.0.2"
// 服务器信息依赖
libraryDependencies += "com.github.oshi" % "oshi-core" % "7.7.1"

// Tapir + Pekko
libraryDependencies ++= Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-pekko-http-server" % "1.13.33",
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.13.33",
  "org.apache.pekko" %% "pekko-http-cors" % "1.4.1"
)

// 日志依赖
libraryDependencies ++= Seq(
  "io.getkyo" %% "kyo-prelude" % "1.0.0-RC7",
  "ch.qos.logback" % "logback-classic" % "1.5.34",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.6"
)

// 数据库依赖
libraryDependencies ++= Seq(
  "com.wz7982" % "sqala-jdbc_3" % "0.7.9",
  "com.zaxxer" % "HikariCP" % "7.1.0",
  // MySQL
  "com.mysql" % "mysql-connector-j" % "8.0.33",
  "org.flywaydb" % "flyway-mysql" % "13.10.0",
  // PostgreSQL
  // "org.postgresql" % "postgresql" % "42.7.13",
  // "org.flywaydb" % "flyway-database-postgresql" % "13.10.0"
)

// JWT 依赖
libraryDependencies ++= Seq(
  "org.mindrot" % "jbcrypt" % "0.4",
  "com.github.jwt-scala" %% "jwt-core" % "11.0.4"
)