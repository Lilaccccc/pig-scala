package pig
package utils.async

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future, Promise}

// 防止同一时间对同一个 Key 发起重复的昂贵操作
class SingleFlight[K, V](using ec: ExecutionContext) {
  private val inFlight = TrieMap.empty[K, Promise[V]]

  def doOnce(key: K)(fn: => Future[V]): Future[V] = {
    val promise = Promise[V]()

    val complete = () => {
      fn.onComplete(result => {
        inFlight.remove(key)
        promise.complete(result)
      })
      promise.future
    }

    inFlight.putIfAbsent(key, promise) match {
      case Some(existing) => existing.future
      case None           => complete()
    }
  }
}