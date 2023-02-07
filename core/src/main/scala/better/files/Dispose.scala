package better.files

import java.util.concurrent.atomic.AtomicBoolean

import scala.collection.GenTraversableOnce
import scala.util.Try
import scala.util.control.NonFatal

/** A typeclass to denote a disposable resource
  * @tparam A
  */
trait Disposable[-A] {
  def dispose(resource: A): Unit

  def disposeSilently(resource: A): Unit = {
    val _ = Try(dispose(resource))
  }
}

object Disposable {
  def apply[A](disposeFunction: A => Any): Disposable[A] =
    new Disposable[A] {
      override def dispose(resource: A) = {
        val _ = disposeFunction(resource)
      }
    }

  def apply[A](disposeMethod: => Unit): Disposable[A] =
    Disposable(_ => disposeMethod)

  implicit val closableDisposer: Disposable[AutoCloseable] =
    Disposable(_.close())

  implicit def traversableDisposer[A](implicit disposer: Disposable[A]): Disposable[Traversable[A]] =
    Disposable(_.foreach(disposer.dispose))

  val fileDisposer: Disposable[File] =
    Disposable(_.delete(swallowIOExceptions = true))
}

/** Given a disposable resource, this actually does the disposing
  */
class Dispose[A](private[Dispose] val resource: A)(implicit disposer: Disposable[A]) {
  private[Dispose] val isDisposed    = new AtomicBoolean(false)
  private[Dispose] def disposeOnce() = if (!isDisposed.getAndSet(true)) disposer.dispose(resource)

  // This is the Scala equivalent of how javac compiles try-with-resources,
  // Except that fatal exceptions while disposing take precedence over exceptions thrown previously
  private[Dispose] def disposeOnceAndThrow(e1: Throwable) = {
    try {
      disposeOnce()
    } catch {
      case NonFatal(e2) => e1.addSuppressed(e2)
      case e2: Throwable =>
        e2.addSuppressed(e1)
        throw e2
    }
    throw e1
  }

  private[Dispose] def withAdditionalDisposeTask[U](f: => U): Dispose[A] =
    new Dispose[A](resource)(Disposable {
      try {
        disposeOnce()
      } finally {
        val _ = f
      }
    })

  /** Apply f to the resource and return it after closing the resource
    * If you don't wish to close the resource (e.g. if you are creating an iterator on file contents), use flatMap instead
    */
  def apply[B](f: A => B): B =
    try {
      f(resource)
    } catch {
      case e1: Throwable => disposeOnceAndThrow(e1)
    } finally {
      disposeOnce()
    }

  /** Dispose this resource and return it
    * Note: If you are using map followed by get, consider using apply instead
    */
  def get(): A =
    apply(identity)

  /** This will immediately apply f on the resource and close the resource
    */
  def foreach[U](f: A => U): Unit = {
    val _ = apply(f)
  }

  /** This will apply f on the resource while it is open
    */
  def map[B](f: A => B): Dispose[B] =
    new Dispose[B](f(resource))(Disposable(disposeOnce()))

  def withFilter(f: A => Boolean): this.type = {
    if (!f(resource)) disposeOnce()
    this
  }

  /** This keeps the resource open during the context of this flatMap and closes when done */
  def flatMap[B, F[_]](f: A => F[B])(implicit fv: Dispose.FlatMap[F]): fv.Output[B] =
    fv.apply(this)(f)
}

object Dispose {
  // Hack to get around issue in Scala 2.11: https://stackoverflow.com/questions/47598333/
  sealed trait FlatMap[-F[_]] {
    type Output[_]
    def apply[A, B](a: Dispose[A])(f: A => F[B]): Output[B]
  }

  object FlatMap {
    trait Implicits {

      /** Compose this managed resource with another managed resource closing the outer one after the inner one */
      implicit object disposeFlatMap extends FlatMap[Dispose] {
        override type Output[X] = Dispose[X]
        override def apply[A, B](m: Dispose[A])(f: A => Dispose[B]) =
          f(m.resource).withAdditionalDisposeTask(m.disposeOnce())
      }

      /** Use the current managed resource as a generator needed to create another sequence */
      implicit object traversableFlatMap extends FlatMap[GenTraversableOnce] {
        override type Output[X] = Iterator[X]
        override def apply[A, B](m: Dispose[A])(f: A => GenTraversableOnce[B]) = {
          val it =
            try {
              f(m.resource).toIterator
            } catch {
              case NonFatal(e) => m.disposeOnceAndThrow(e)
            }
          it withHasNext {
            try {
              val result = it.hasNext
              if (!result) m.disposeOnce()
              result
            } catch {
              case e1: Throwable => m.disposeOnceAndThrow(e1)
            }
          }
        }
      }
    }
  }
}
