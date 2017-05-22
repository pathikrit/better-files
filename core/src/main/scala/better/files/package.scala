package better

import java.io.InputStream

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

package object files extends Implicits {

  /**
    * Default array buffer size
    * Seems like a good value used by JDK: (see: java.io.BufferedInputStream.DEFAULT_BUFFER_SIZE)
    */
  private[files] val defaultBufferSize = 8192

  type Files = Iterator[File]

  type Disposable = {
    def close(): Unit
  }

  def resourceAsStream(name: String): InputStream = currentClassLoader().getResourceAsStream(name)

  // Some utils:
  private[files] def newMultiMap[A, B]: mutable.MultiMap[A, B] = new mutable.HashMap[A, mutable.Set[B]] with mutable.MultiMap[A, B]

  @inline private[files] def when[A](condition: Boolean)(f: => A): Option[A] = if (condition) Some(f) else None

  @inline private[files] def repeat[U](n: Int)(f: => U): Unit = (1 to n).foreach(_ => f)

  private[files] def currentClassLoader() = Thread.currentThread().getContextClassLoader

  private[files] def using[A <: Disposable, U](resource: A)(f: A => U): U = try { f(resource) } finally {resource.close()}

  private[files] def produce[A](f: => A) = new {
    def till(hasMore: => Boolean): Iterator[A] = new Iterator[A] {
      override def hasNext = hasMore
      override def next() = f
    }
  }

  /**
    * Utility to apply f on all xs skipping over errors
    * Throws the last error that happened
    * *
    * @param xs
    * @param f
    * @tparam A
    */
  private[files] def tryAll[A](xs: Seq[A])(f: A => Unit): Unit = {
    val res = xs.foldLeft(Option.empty[Throwable]) {
      case (currError, a) =>
        Try(f(a)) match {
          case Success(_) => currError
          case Failure(e) => Some(e)
        }
    }
    res.foreach(throwable => throw throwable)
  }
}
