package pig
package utils.redis

import io.circe.parser.*
import io.circe.{Decoder, Encoder}
import redis.clients.jedis.params.*
import utils.base.Converters.*
import utils.base.config.enums.RedisConfig
import utils.base.json
import utils.redis.RedisFactory.redisClient

import com.typesafe.scalalogging.LazyLogging

object RedisOps extends LazyLogging {
  private lazy val keyPrefix: String = s"${RedisConfig.keyPrefix}:"

  extension (key: String) {
    private def addKeyPrefix: String = s"$keyPrefix$key"
  }
  
  def client = redisClient

  def set[T: Encoder](key: String, value: T, ttlSeconds: Long = -1L): Unit = try {
    val jsonStr = value.json
    if ttlSeconds == -1L then redisClient.set(key.addKeyPrefix, jsonStr, SetParams())
    else redisClient.set(key.addKeyPrefix, jsonStr, SetParams().ex(ttlSeconds))
  } catch {
    case e: Exception => logger.warn(s"Redis set failed for key=$key: ${e.getMessage}")
  }

  def hset[T: Encoder](key: String, hash: Map[String, T], ttlSeconds: Long = -1L): Unit = try {
    val map = hash.map((k, v) => (k, v.json)).toJava
    if ttlSeconds == -1L then redisClient.hset(key.addKeyPrefix, map)
    else redisClient.hsetex(key.addKeyPrefix, HSetExParams().ex(ttlSeconds), map)
  } catch {
    case e: Exception => logger.warn(s"Redis hset failed for key=$key: ${e.getMessage}")
  }

  def mset[T: Encoder](keyValues: Map[String, T], ttlSeconds: Long = -1L): Unit = try {
    val array = keyValues.toArray.flatMap((k, v) => Array(k.addKeyPrefix, v.json))
    if ttlSeconds == -1L then redisClient.mset(array*)
    else redisClient.msetex(MSetExParams().ex(ttlSeconds), array*)
  } catch {
    case e: Exception => logger.warn(s"Redis mset failed: ${e.getMessage}")
  }

  def get[T: Decoder](key: String): Option[T] = try {
    val raw = redisClient.get(key.addKeyPrefix)
    if raw == null then None
    else decode[T](raw) match
      case Right(value) => Some(value)
      case Left(e)      => None
  } catch {
    case e: Exception =>
      logger.warn(s"Redis get failed for key=$key: ${e.getMessage}")
      None
  }

  def hget[T: Decoder](key: String, field: String): Option[T] = try {
    val raw = redisClient.hget(key.addKeyPrefix, field)
    if raw == null then None
    else decode[T](raw) match
      case Right(value) => Some(value)
      case Left(e)      => None
  } catch {
    case e: Exception =>
      logger.warn(s"Redis hget failed for key=$key: ${e.getMessage}")
      None
  }

  def mget[T: Decoder](key: List[String]): List[T] = try {
    redisClient.mget(key.map(_.addKeyPrefix)*).toScala.flatMap(decode[T](_).toOption)
  } catch {
    case e: Exception =>
      logger.warn(s"Redis mget failed: ${e.getMessage}")
      List.empty
  }

  def exists(key: String): Boolean = try {
    redisClient.exists(key.addKeyPrefix)
  } catch {
    case e: Exception =>
      logger.warn(s"Redis exists failed for key=$key: ${e.getMessage}")
      false
  }

  def init(): Unit = if RedisConfig.clearnOnStart then {
    deleteByPrefix("")
    logger.info("初始化 Redis 缓存完毕")
  }

  // 删除指定键前缀的键
  def deleteByPrefix(prefix: String, count: Int = 100): Unit = {
    var cursor     = ScanParams.SCAN_POINTER_START
    val scanParams = ScanParams()
    scanParams.`match`(s"$keyPrefix$prefix*")
    scanParams.count(count)

    while {
      val scan = redisClient.scan(cursor, scanParams)
      val keys = scan.getResult
      cursor = scan.getCursor
      if !keys.isEmpty then {
        val pipeline = redisClient.pipelined
        keys.forEach(key => pipeline.del(key))
        pipeline.sync()
      }
      cursor != ScanParams.SCAN_POINTER_START
    } do ()
  }

  def delete(key: String): Unit = redisClient.del(key.addKeyPrefix)

  def zIncrBy(key: String, member: String, incr: Double, ttlSeconds: Long = -1L): Unit = try {
    redisClient.zincrby(key, incr, member)
    if ttlSeconds != -1L then redisClient.expire(key, ttlSeconds)
  } catch {
    case e: Exception => logger.warn(s"Redis zIncrBy failed for key=$key: ${e.getMessage}")
  }
}