package better.files

import java.util.concurrent.atomic.AtomicBoolean

import scala.collection.compat._
import scala.util.Try

/** A typeclass to denote a disposable resource */
trait Disposable[-A] {
  def dispose(resource: A): Unit

  def disposeSilently(resource: A): Unit =
    Try(dispose(resource))

}

object Disposable {
  def apply[A](disposeFunction: A => Any): Disposable[A] =
    new Disposable[A] {
      override def dispose(resource: A) =
        disposeFunction(resource)
    }

  def apply[A](disposeMethod: => Unit): Disposable[A] =
    Disposable(_ => disposeMethod)

  implicit val closableDisposer: Disposable[AutoCloseable] =
    Disposable(_.close())

  implicit def traversableDisposer[A](implicit disposer: Disposable[A]): Disposable[Iterable[A]] =
    Disposable(_.foreach(disposer.dispose))

  val fileDisposer: Disposable[File] =
    Disposable(_.delete(swallowIOExceptions = true))
}

/** Given a disposable resource, this actually does the disposing */
class Dispose[A](private[Dispose] val resource: A)(implicit disposer: Disposable[A]) {
  private[Dispose] val disposeOnce = Once(() => disposer.dispose(resource))

  private[Dispose] def withAdditionalDisposeTask[U](f: => U): Dispose[A] =
    new Dispose[A](resource)(Disposable {
      try {
        disposeOnce()
      } finally {
        f: Unit
      }
    })

  /** Apply f to the resource and return it after closing the resource
    * If you don't wish to close the resource (e.g. if you are creating an iterator on file contents), use flatMap instead
    */
  def apply[B](f: A => B): B =
    tryWith(f(resource), disposeOnce, finallyClose = true)

  /** Dispose this resource and return it
    * Note: If you are using map followed by get, consider using apply instead
    */
  def get(): A =
    apply(identity)

  /** This will immediately apply f on the resource and close the resource */
  def foreach[U](f: A => U): Unit =
    apply(f): Unit

  /** This will apply f on the resource while it is open */
  def map[B](f: A => B): Dispose[B] =
    new Dispose[B](f(resource))(Disposable(disposeOnce()))

  def withFilter(f: A => Boolean): this.type = {
    if (!f(resource)) disposeOnce()
    this
  }

  /** Generate a self closing iterator from this disposable resource */
  def iterator[B](f: A => Iterator[B]): Iterator[B] =
    CloseableIterator(f(resource), disposeOnce)

  /** This keeps the resource open during the context of this flatMap and closes when done */
  def flatMap[B, F[_]](f: A => F[B])(implicit fv: Dispose.FlatMap[F]): fv.Output[B] =
    fv.apply(this)(f)
}

object Dispose {
  // TODO: rm this hack once we drop Scala 2.11 (see: https://stackoverflow.com/questions/47598333/)
  sealed trait FlatMap[-F[_]] {
    type Output[_]
    def apply[A, B](a: Dispose[A])(f: A => F[B]): Output[B]
  }

  object FlatMap {
    trait Implicits {

      /** Compose this managed resource with another managed resource closing the outer one after the inner one */
      implicit object dispose extends FlatMap[Dispose] {
        override type Output[X] = Dispose[X]
        override def apply[A, B](m: Dispose[A])(f: A => Dispose[B]) =
          f(m.resource).withAdditionalDisposeTask(m.disposeOnce())
      }

      /** Use the current managed resource as a generator needed to create another sequence */
      implicit object iterable extends FlatMap[IterableOnce] {
        override type Output[X] = Iterator[X]
        override def apply[A, B](m: Dispose[A])(f: A => IterableOnce[B]) =
          m.iterator(f(_).iterator)
      }
    }
  }
}

/** Converts a given function to something that can be called only once */
private[files] case class Once(f: () => Unit) extends (() => Unit) {
  private[this] val flag = new AtomicBoolean(false)

  override def apply() = if (!flag.getAndSet(true)) f()

  def isInvoked(): Boolean = flag.get()
}
