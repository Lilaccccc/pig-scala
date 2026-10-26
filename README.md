# pig-scala

> 个人自用的 Scala 3 后端快速启动模板：复制仓库、改配置、写 Controller，就能开始做业务。

这个模板把「起一个后端服务」需要的通用能力提前搭好了：HTTP 路由、统一响应、错误处理、链路追踪、数据库迁移、Redis、配置管理、日志、ID 生成和打包；它不绑定具体业务，适合个人项目、内部工具和 API 原型。

## 特性

- **声明式路由**：用 Tapir 定义 endpoint，输入输出类型安全，接口结构一目了然。
- **Controller 自动发现**：编译期宏扫描 `pig` 包下所有 `Controller` 实现，启动时自动实例化，不需要手写路由注册表。
- **统一响应体**：所有接口返回 `Result[T]`（`code` / `msg` / `data`），框架层错误（解码失败、404）也走同一结构。
- **业务错误约定**：`ErrorResult` + `StatusCode` 枚举，业务错误码集中管理。
- **链路追踪**：读取或生成 `X-Trace-Id`，写入 MDC 和响应头，日志模板已带 traceId。
- **数据层就绪**：HikariCP + sqala-jdbc + Flyway 版本化迁移，默认 MySQL。
- **Redis 工具层**：Jedis 连接池 + Circe JSON 序列化 + 统一 key 前缀，大部分读写封装失败时只记录告警。
- **多环境配置**：Typesafe Config，`active` 切换 `dev` / `prod`，配置项通过枚举 + 内联方法强类型访问。
- **并发小工具**：MDC 透传线程池、`SingleFlight`（合并相同 key 的并发请求）、后台任务执行器。
- **基础组件**：雪花 ID（含时钟回拨补偿）、HTTP 客户端、JVM/系统指标、彩色日志。
- **一键打包**：sbt-assembly 已配置合并策略，输出可直接 `java -jar` 运行的 fat jar，并注册了优雅关闭。

## 技术栈

| 分类 | 选型 | 版本 |
| --- | --- | --- |
| 语言 | Scala 3 | 3.10.0 |
| 构建 | sbt / sbt-assembly / sbt-ide-settings | 2.0.10 / 2.5.0 / 1.1.4 |
| HTTP | Pekko HTTP + pekko-http-cors | 传递依赖 / 1.4.1 |
| API 描述 | Tapir（Pekko 后端 + Circe JSON） | 1.13.33 |
| JSON | Circe | 随 Tapir 引入 |
| 数据库 | MySQL Connector/J / HikariCP / Flyway | 8.0.33 / 7.1.0 / 13.10.0 |
| SQL 访问 | sqala-jdbc | 0.7.9 |
| 缓存 | Jedis | 8.0.2 |
| 日志 | Logback + scala-logging | 1.5.34 / 3.9.6 |
| 系统监控 | OSHI | 7.7.1 |
| 对象转换 | Chimney | 2.1.0 |
| 认证相关 | jBCrypt + jwt-core（仅有依赖和配置，未实现） | 0.4 / 11.0.4 |
| 终端着色 | Kyo `kyo-prelude`（只用到 ANSI 颜色） | 1.0.0-RC7 |

## 目录结构

源码采用「平铺 + 包前缀」的方式：文件直接放在 `src/main/scala` 下，包名由源码中的 `package pig` 声明，IDE 通过 `idePackagePrefix := Some("pig")` 识别。新增文件放在 `src/main/scala/<子包>/` 即可。

```text
.
├── build.sbt                        # 依赖、assembly 合并策略、主类
├── project/
│   ├── build.properties             # sbt 2.0.10
│   └── plugins.sbt                  # sbt-assembly、sbt-ide-settings
├── .scalafmt.conf                   # Scala 3 格式化配置
└── src/main/
    ├── resources/
    │   ├── application.conf         # 只负责选择环境：active = dev
    │   ├── application-dev.conf     # 开发环境配置
    │   ├── application-prod.conf    # 生产环境配置
    │   ├── logback.xml              # 控制台日志，格式含 traceId
    │   └── db/migration/
    │       └── V1__init.sql         # Flyway 迁移脚本（用户表）
    └── scala/
        ├── Application.scala        # 程序入口
        ├── internal/
        │   ├── InitService.scala    # 启动编排：迁移 → Redis → HTTP
        │   └── RouteOps.scala       # Endpoint 扩展：统一 Result 包装
        └── utils/
            ├── async/               # 线程池（MDC 透传）、SingleFlight
            ├── base/
            │   ├── config/          # Typesafe Config 强类型读取
            │   ├── idgenerator/     # 雪花 ID、随机十六进制 ID
            │   ├── ColoredLog.scala # ANSI 彩色日志
            │   ├── Converters.scala # Java / Scala 集合互转
            │   ├── HttpClient.scala # 基于 Pekko 的 HTTP 客户端
            │   └── Json.scala       # `.json` / `.decode[T]` 扩展
            ├── cmd/                 # Windows bat 执行
            ├── db/                  # HikariCP + sqala + Flyway
            ├── health/              # JVM / 系统指标
            ├── redis/               # Jedis 客户端与 RedisOps
            ├── result/              # Result / ErrorResult / StatusCode
            ├── route/               # Controller / HttpService / 路由工具与拦截器
            ├── thread/              # 后台任务执行器
            └── time/                # 时间工具
```

## 启动流程

```text
Application.main
└─ InitService.apply()
   ├─ migrate()                       # Flyway 迁移（migrateOnStart = true 时）
   ├─ RedisOps.init()                 # clearnOnStart = true 时清空本服务前缀缓存
   └─ startHttp()
      ├─ initControllers[Controller]  # 编译期宏：扫描并实例化所有 Controller
      ├─ RouteUtil.toRoute(...)       # Tapir 端点 → Pekko Route（CORS / TraceId / 解码失败处理）
      └─ HttpService.init(...)        # 监听 0.0.0.0:8080，注册优雅关闭
```

数据库和 Redis 客户端都是懒加载：开发环境不开启自动迁移、也不访问缓存时，可以先把服务空跑起来。

## 快速开始

### 环境要求

- **JDK 21+**：`utils/db/Db.scala` 使用了虚拟线程（`Thread.ofVirtual()`）。
- **sbt 2.x**：`project/build.properties` 已固定 2.0.10，首次运行会自动下载。
- **MySQL 8.x**：默认连接 `127.0.0.1:3306/pig`，账号 `root / 123456`。
- **Redis 8.x**：默认连接 `127.0.0.1:6380`（注意不是 6379）。带 TTL 的 hash 写入用到 Redis 8.0 引入的 `HSETEX`，带 TTL 的批量写入用到 Redis 8.4 引入的 `MSETEX`，建议直接用 8.4+。

### 启动步骤

1. 修改 `src/main/resources/application-dev.conf`：数据库、Redis、端口、CORS、JWT secret。
2. 首次建表二选一：把 `hikari.migrateOnStart` 改为 `true`（启动时由 Flyway 自动执行 `V1__init.sql`），或者手动执行 `src/main/resources/db/migration/V1__init.sql`。开发环境默认 `migrateOnStart = false`，生产环境默认 `true`。
3. 启动服务：

   ```bash
   sbt "runMain pig.Application"
   ```

   项目里还有 `IdGenerator`、`SystemHealth` 两个带 `main` 的工具对象，直接 `sbt run` 会提示选择主类，所以这里显式指定入口。

4. 控制台出现 `服务器启动成功: /0.0.0.0:8080` 后，访问 `http://localhost:8080/`。此时还没有业务路由，但应该返回统一的 404 JSON（`code = 1004`），说明服务和错误处理链路都正常。
5. 按下面的示例添加 Controller，重启后接口自动生效。

### 常用命令

| 命令 | 说明 |
| --- | --- |
| `sbt "runMain pig.Application"` | 本地启动服务 |
| `sbt compile` | 编译 |
| `sbt "show discoveredMainClasses"` | 查看可运行主类（当前有 Application、IdGenerator、SystemHealth） |
| `sbt clean assembly` | 打包 fat jar，产物在 `target/` 下的 `pig-scala-assembly-*.jar` |

## 配置说明

`application.conf` 只保存一个 `active`，用于选择实际加载的配置文件：

```hocon
active = dev   # 可选 dev / prod
```

| 配置段 | 内容 |
| --- | --- |
| `server` | 端口、服务名、`poolSize`（`Future` 固定线程池大小，不是 HTTP 线程数） |
| `hikari` | JDBC URL、账号密码、连接池参数、启动时是否迁移 |
| `redis` | 地址端口、密码、库号、连接池与超时、key 前缀、启动清空开关 |
| `jwt` | secret、access / refresh 有效期（目前仅配置预留） |
| `pekko.http.cors` | 允许来源、方法、请求头、凭证等 |
| `active` | 环境选择 |

几个容易踩的点：

- `redis.keyPrefix` 默认取 `${server.name}`，实际写入的 key 会再加一个 `:`，即 `pig_scala:xxx`。
- `redis.clearnOnStart` 开启后，启动时会删除当前前缀下的所有缓存，适合本地调试，慎用于生产。
- `pekko.http.cors.allowed-origins` 默认为空，前后端分离联调时需要补上来源。
- `dev` 和 `prod` 两份配置的键要保持一致：访问不存在的配置项会直接抛异常。
- 配置文件打包在 jar 内。切换环境可以改 `active` 或在启动时用 `-Dactive=prod` 覆盖；单个配置项也可以用 `-Dserver.port=9090` 这类方式覆盖。
- 默认数据库密码、JWT secret 都是占位值，部署到公网前务必替换，不要提交真实密钥。

新增配置项的步骤：在 `utils/base/config/enums/` 下新建或修改枚举 → 继承 `Config("配置段")` 声明字段 → 在伴生对象里暴露访问方法 → 同步补充 dev / prod 配置文件。

## 写第一个接口

在 `src/main/scala/controller/HelloController.scala` 新建：

```scala
package pig
package controller

import internal.RouteOps.*
import utils.result.{ErrorResult, Result, StatusCode}
import utils.route.Controller

import scala.concurrent.Future

class HelloController extends Controller {
  override val serverEndpointList = List(hello, user)
  
  private val hello = endpoint.get
    .in("hello")
    .in(query[String]("name"))
    .out(jsonBody[Result[String]])
    .logicWithResult(name => s"hello, $name")

  private val user = endpoint.get
    .in("user" / path[Long]("id"))
    .out(jsonBody[Result[String]])
    .logicWithEitherAsync { id =>
      Future.successful(
        if id <= 0 then Left(BizError(StatusCode.ParamError.code, "id 必须为正数"))
        else Right(s"user-$id")
      )
    }
}

final case class BizError(override val code: Int, override val message: String)
  extends ErrorResult(code, message)
```

请求效果：

```text
GET /hello?name=world
→ {"code":1000,"msg":"请求成功","data":"hello, world"}
```

Controller 的发现规则：

- 必须位于根包 `pig` 之下（任意子包都行），实现 `utils.route.Controller`。
- 必须有无参主构造器，因为宏在启动时通过 `primaryConstructor` 直接实例化；构造器里的代码会在启动阶段执行。
- 自动发现发生在编译期，Controller 需要和入口在同一个模块（当前就是 `src/main/scala`）。

`RouteOps` 提供四种端点包装方式：

| 方法 | 场景 | 业务函数签名 |
| --- | --- | --- |
| `logic` | 不需要统一响应体，直接返回 `O` | `I => O` |
| `logicWithResult` | 同步逻辑，自动包装为 `Result[O]` | `I => O` |
| `logicWithResultAsync` | 异步逻辑，自动包装为 `Result[O]` | `I => Future[O]` |
| `logicWithEitherAsync` | 异步逻辑 + 显式业务错误 | `I => Future[Either[ErrorResult, O]]` |

补充约定：

- `logicWithResult` 返回的对象如果本身是 `ErrorResult` 子类，会被转换成对应错误码；抛出的异常统一变成 `1001`，并打印错误日志。
- `ErrorResult` 的构造函数里带日志：创建业务错误对象时会自动记录一条 error 日志。
- 业务错误的 HTTP 状态码仍然是 200，客户端需要根据响应体里的 `code` 判断结果。

## 统一响应与状态码

成功和业务失败都使用 `Result`：

```json
{ "code": 1000, "msg": "请求成功", "data": {} }
```

| code | 含义 |
| --- | --- |
| 1000 | 请求成功 |
| 1001 | 请求失败（默认错误、未捕获异常） |
| 1002 | 参数错误 |
| 1003 | 未授权 |
| 1004 | 资源不存在 |
| 1005 | 禁止访问 |
| 1006 | 未查询到数据 |
| 1007 | 用户已存在 |
| 1008 | 用户不存在 |
| 1009 | 用户名或密码错误 |
| 1010 | 账号已被禁用 |
| 1011 | 登录状态已失效 |

框架层错误也已经统一：

- 未匹配的路径（404）返回 HTTP 200 + `code 1004`。
- 请求体解析失败、参数缺失、参数类型不匹配等由 `CodeFailureHandler` 转成统一 JSON，`msg` 中附带具体原因。
- 命中接口的请求会带 `X-Trace-Id`：优先使用请求头里的值，否则生成雪花 ID；响应头会回传，日志通过 MDC 打印。

## 数据访问与迁移

- 连接池使用 HikariCP，线程工厂换成虚拟线程。
- SQL 访问使用 sqala-jdbc，默认方言为 `MysqlDialect`。`utils.db` 额外提供了字符串的 `.sql(...)` 扩展，用于构造原生 SQL：

  ```scala
  import sqala.jdbc.Logger
  import utils.db.*

  given Logger = Logger(sql => println(sql))

  val rows: List[User] = db.fetchTo[User](
    "SELECT id, username, password, nickname, status, create_time, update_time FROM t_user WHERE id = ?".sql(1L)
  )
  ```

  - 使用前需要在作用域内提供 `sqala.jdbc.Logger`。
  - `fetchTo[T]` 的目标类型需要是 `Product`（通常是 case class），且查询列的顺序要与字段顺序一致。
  - 原生 SQL 不要把前端参数直接拼进字符串（表名等后端可控内容除外），通过 `.sql(args*)` 的 `?` 占位符传参，避免 SQL 注入。

- 迁移脚本放在 `src/main/resources/db/migration/`，命名 `V{版本}__{描述}.sql`，Flyway 按版本顺序执行。`V1__init.sql` 目前只建了一张 `t_user` 表，不需要可以直接删掉。
- 默认启用 `baselineOnMigrate`（基线版本 0），因此对已有非空数据库也能从 `V1` 开始接管。
- 开发环境默认不自动迁移，生产环境默认自动迁移；迁移在 HTTP 启动前执行，失败会阻止服务启动。
- 如需切换 PostgreSQL：在 `build.sbt` 中换掉 MySQL 依赖、启用 PostgreSQL 依赖，在 `utils/db/Db.scala` 中改用 `PostgresqlDialect`，并注意现有迁移脚本是 MySQL 语法。

## Redis

```scala
import utils.redis.RedisOps

RedisOps.set("user:1", user, ttlSeconds = 300)  // 值按 Circe JSON 序列化
val cached = RedisOps.get[User]("user:1")       // 类型需要 Decoder
```

- 已封装 `set` / `hset` / `mset` / `get` / `hget` / `mget` / `exists` / `delete` / `deleteByPrefix` / `zIncrBy`。
- key 自动加 `${server.name}:` 前缀（默认 `pig_scala:`），避免多个服务共用一个 Redis 时互相覆盖。
- 大部分读写方法内部捕获异常并打印 warn，缓存故障不会直接把接口打挂；`delete`、`deleteByPrefix` 等少量方法没有做兜底。
- `RedisOps.init()` 仅在 `clearnOnStart = true` 时真正连接 Redis 并清理缓存。

## 常用工具

| 位置 | 能力 |
| --- | --- |
| `utils/async/Ec.scala` | 固定大小守护线程池，提交任务时透传 MDC |
| `utils/async/SingleFlight.scala` | 相同 key 的并发请求只执行一次昂贵操作 |
| `utils/thread/Task.scala` | 后台任务执行器，自动继承 traceId |
| `utils/base/HttpClient.scala` | 基于 Pekko 的 GET / POST / HEAD 请求，JSON 自动编解码 |
| `utils/base/idgenerator/` | 雪花 ID（时钟回拨补偿）和随机十六进制 ID |
| `utils/base/Json.scala` | `.json` 序列化、`.decode[T]` 反序列化扩展 |
| `utils/base/Converters.scala` | Java / Scala 集合互转 |
| `utils/base/ColoredLog.scala` | `.e` / `.w` / `.i` / `.d` 彩色日志文案 |
| `utils/health/` | JVM 状态、CPU、内存、磁盘、负载、运行时长 |
| `utils/cmd/Cmd.scala` | 执行 Windows bat 脚本 |
| `utils/time/TimeUtils.scala` | 计算某个月的第一秒和最后一秒 |

## 打包与部署

```bash
sbt clean assembly

# 产物位于 target/ 下，文件名类似 pig-scala-assembly-0.1.0-SNAPSHOT.jar
java -Dactive=prod -jar <path>/pig-scala-assembly-0.1.0-SNAPSHOT.jar
```

- assembly 的合并策略已经在 `build.sbt` 中配置好（处理了 `META-INF`、`google.protobuf`、`smile.plot.vega`、`module-info.class` 等常见冲突），遇到新的依赖冲突时再按需补充。
- 服务通过 Pekko 的 `CoordinatedShutdown` 在收到关闭信号时先解绑端口，再退出进程。
- 日志默认只输出到控制台，级别为 DEBUG；`logback.xml` 里已经写好文件 Appender，按需取消注释即可写入 `./logs/application.log`。
- 本地依赖可以直接把 jar 扔进项目根目录的 `lib/` 文件夹（`unmanagedBase` 已指向这里）。

### 多实例部署注意

雪花 ID 在 `IdGenerator` 中固定使用 `workerId = 0, datacenterId = 0`。单实例没问题，部署多个实例前必须为每台机器配置不同的值，否则同一毫秒内可能生成重复 ID。

## 模板使用清单

复制这个项目开新坑时，按顺序做这几件事：

1. 改 `build.sbt`：项目名 `name`、`idePackagePrefix`、`assembly / mainClass`。
2. 如需换根包名，全局替换 `package pig` / `import pig...`；Controller 自动发现会从入口自动推断根包，不需要额外配置。
3. 改 `application-dev.conf` / `application-prod.conf`：端口、服务名、数据库、Redis、CORS、JWT secret。
4. 处理 `V1__init.sql`：不需要用户表就删掉，后续新增自己的 `V2__xxx.sql`。
5. 添加 Controller，跑起来验证接口。
6. 清理暂时用不到的依赖（如认证、Chimney、并行集合等），减小包体积。

## 尚未内置

- **登录鉴权**：JWT 依赖、`JwtConfig` 和 jBCrypt 已经备好，但没有 token 签发/校验、登录接口和用户服务。
- **测试**：当前没有 `src/test`，也没有引入测试框架。
- **健康检查接口**：`JvmHealth` / `SystemHealth` 只是工具类，没有暴露成 HTTP 端点。
- **指标采集**：`JvmStatus.gcCount` 目前固定为 0，未实际采集 GC 次数。

## 已知限制

- 所有业务响应都返回 HTTP 200，错误靠响应体 `code` 区分，客户端封装时不要只看 HTTP 状态码。
- `SystemHealth.cpu` 每次调用会阻塞约 1 秒做采样，如果要暴露成接口，注意放到合适的线程池。
- `Cmd.executeBatFile` 仅适用于 Windows。
- `RedisOps` 中带 TTL 的 `hset` 需要 Redis 8.0+，带 TTL 的 `mset` 需要 Redis 8.4+。
- 优雅关闭钩子只解绑了 HTTP 端口；Redis 客户端（`RedisFactory.shutdown`）和 HikariCP 连接池没有接入关闭流程，需要时自行注册。
