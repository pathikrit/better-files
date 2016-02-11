package better

import scala.collection.mutable

package object files extends Implicits {
  type Files = Iterator[File]

  type Closeable = {
    def close(): Unit
  }

  type ManagedResource[A <: Closeable] = Traversable[A]

  // Some utils:
  @inline private[files] def when[A](condition: Boolean)(f: => A): Option[A] = if (condition) Some(f) else None
  @inline private[files] def repeat(n: Int)(f: => Unit): Unit = (1 to n).foreach(_ => f)
  @inline private[files] def returning[A](obj: A)(f: => Unit): A = {f; obj}
  private[files] def newMultiMap[A, B]: mutable.MultiMap[A, B] = new mutable.HashMap[A, mutable.Set[B]] with mutable.MultiMap[A, B]
}
