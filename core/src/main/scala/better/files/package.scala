package better

import scala.collection.mutable

package object files extends Implicits {
  type Files = Iterator[File]

  type Closeable = {
    def close(): Unit
  }

  type ManagedResource[A <: Closeable] = Traversable[A]

  // Some utils:
  private[files] def newMultiMap[A, B]: mutable.MultiMap[A, B] = new mutable.HashMap[A, mutable.Set[B]] with mutable.MultiMap[A, B]

  @inline private[files] def when[A](condition: Boolean)(f: => A): Option[A] = if (condition) Some(f) else None
  @inline private[files] def repeat[U](n: Int)(f: => U): Unit = (1 to n).foreach(_ => f)

  private[files] def produce[A](f: => A) = new {
    def till(hasMore: => Boolean): Iterator[A] = new Iterator[A] {
      override def hasNext = hasMore
      override def next() = f
    }
  }
}
