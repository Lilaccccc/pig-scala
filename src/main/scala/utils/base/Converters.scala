package pig
package utils.base

import scala.jdk.CollectionConverters.*

object Converters {
  extension [T](javaList: java.util.List[T]) {
    def toScala: List[T] = javaList.asScala.toList
  }

  extension [T](scalaList: List[T]) {
    def toJava: java.util.List[T] = scalaList.asJava
  }

  extension [K, V](javaMap: java.util.Map[K, V]) {
    def toScala: Map[K, V] = javaMap.asScala.toMap
  }

  extension [K, V](scalaMap: Map[K, V]) {
    def toJava: java.util.Map[K, V] = scalaMap.asJava
  }
}
