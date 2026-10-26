package pig
package utils.redis

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import utils.base.config.enums.RedisConfig
import redis.clients.jedis.{Connection, DefaultJedisClientConfig, RedisClient}

import java.time.Duration
import java.time.temporal.ChronoUnit

object RedisFactory {
  lazy val redisClient: RedisClient = {
    // 构建连接池配置
    val poolConfig = new GenericObjectPoolConfig[Connection]()
    poolConfig.setMaxTotal(RedisConfig.maxTotal)
    poolConfig.setMaxIdle(RedisConfig.maxIdle)
    poolConfig.setMinIdle(RedisConfig.minIdle)
    poolConfig.setEvictorShutdownTimeout(Duration.of(RedisConfig.evictorShutdownTimeout, ChronoUnit.MILLIS))
    poolConfig.setTestOnBorrow(RedisConfig.testOnBorrow)
    poolConfig.setTestOnReturn(RedisConfig.testOnReturn)
    poolConfig.setTestWhileIdle(RedisConfig.testWhileIdle)

    // 创建 RedisClient
    RedisClient.builder
      .hostAndPort(RedisConfig.host, RedisConfig.port)
      .clientConfig(
        DefaultJedisClientConfig.builder
          .password(RedisConfig.password)
          .database(RedisConfig.database)
          .timeoutMillis(RedisConfig.timeoutMillis)
          .connectionTimeoutMillis(RedisConfig.connectionTimeoutMillis)
          .socketTimeoutMillis(RedisConfig.socketTimeoutMillis)
          .blockingSocketTimeoutMillis(RedisConfig.blockingSocketTimeoutMillis)
          .build
      )
      .poolConfig(poolConfig)
      .build
  }

  // 关闭线程池
  def shutdown: Unit = redisClient.close()
}
