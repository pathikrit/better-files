package better

import java.io.StreamTokenizer
import java.nio.charset.Charset

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

package object files extends Implicits {

  /**
    * Default array buffer size
    * Seems like a good value used by JDK: (see: java.io.BufferedInputStream.DEFAULT_BUFFER_SIZE)
    */
  val DefaultBufferSize = 8192

  /**
    * The Default charset used by better-files
    * Note: It uses java.net.charset.Charset.DefaultCharset() in general but if the Default supports byte-order markers,
    *       it uses a more compliant version than the JDK one (see: https://github.com/pathikrit/better-files/issues/107)
    */
  val DefaultCharset: Charset =
    UnicodeCharset(Charset.defaultCharset())

  val EOF = StreamTokenizer.TT_EOF

  /**
    * Similar to the `with` keyword in Python and `using` keyword in .NET and `try-with-resource` syntax in Java,
    * this let's you use and dispose a resource e.g.
    *
    * {{
    *     val lines: List[String] = using(file.newInputStream) { stream =>
    *         stream.lines.toList   // Must be eager so .toList
    *     }
    * }}
    *
    * @param resource
    * @param f
    * @tparam A
    * @tparam B
    * @return
    */
  def using[A: Disposable, B](resource: A)(f: A => B): B =
    new Dispose(resource).apply(f)

  // Some utils:
  private[files] def newMultiMap[A, B]: mutable.MultiMap[A, B] =
    new mutable.HashMap[A, mutable.Set[B]] with mutable.MultiMap[A, B]

  @inline private[files] def when[A](condition: Boolean)(f: => A): Option[A] = if (condition) Some(f) else None

  @inline private[files] def repeat[U](n: Int)(f: => U): Unit = (1 to n).foreach(_ => f)

  private[files] def eofReader(read: => Int): Iterator[Int] = Iterator.continually(read).takeWhile(_ != EOF)

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

  private[files] def toHex(bytes: Array[Byte]): String =
    String.format("%0" + (bytes.length << 1) + "X", new java.math.BigInteger(1, bytes))
}
