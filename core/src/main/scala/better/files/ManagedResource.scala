package better.files

import java.util.concurrent.atomic.AtomicBoolean

import scala.util.Try
import scala.util.control.NonFatal

/**
  * A typeclass to denote a disposable resource
  * @tparam A
  */
trait Disposable[-A]  {
  def dispose(resource: A): Unit

  def disposeSilently(resource: A): Unit = {
    val _ = Try(dispose(resource))
  }
}

object Disposable {
  def apply[A](disposeMethod: A => Any): Disposable[A] = new Disposable[A] {
    override def dispose(resource: A) = {
      val _ = disposeMethod(resource)
    }
  }

  implicit val closableDisposer: Disposable[AutoCloseable] =
    Disposable(_.close())

  val fileDisposer: Disposable[File] =
    Disposable(_.delete(swallowIOExceptions = true))
}

class ManagedResource[A](resource: A)(implicit disposer: Disposable[A]) {
  private[this] val isDisposed = new AtomicBoolean(false)
  private[this] def disposeOnce() = if (!isDisposed.getAndSet(true)) disposer.dispose(resource)

  def foreach[U](f: A => U): Unit = {
    val _ = map(f)
  }

  def map[B](f: A => B): B = {
    // Avoid using Option here. If an OutOfMemoryError is caught, allocating an Option may cause another one.
    var e1: Throwable = null

    // This is the Scala equivalent of how javac compiles try-with-resources, except that fatal exceptions while disposing take precedence over exceptions thrown by the provided function.
    try
      f(resource)
    catch { case e: Throwable =>
      e1 = e
      throw e
    }
    finally {
      if (e1 ne null) {
        try
          disposeOnce()
        catch { case e2: Throwable =>
          if (NonFatal(e2))
            e1.addSuppressed(e2)
          else {
            e2.addSuppressed(e1)
            throw e2
          }
        }
      }
      else
        disposeOnce()
    }
  }

  def withFilter(f: A => Boolean): this.type = {
    if (!f(resource)) disposeOnce()
    this
  }

  /**
    * This handles lazy operations (e.g. Iterators)
    * for which resource needs to be disposed only after iteration is done
    *
    * @param f
    * @tparam B
    * @return
    */
  def flatMap[B](f: A => Iterator[B]): Iterator[B] = {
    val it = f(resource)
    it withHasNext {
      try {
        val result = it.hasNext
        if (!result) disposeOnce()
        result
      }
      catch { case e1: Throwable =>
        try
          disposeOnce()
        catch { case e2: Throwable =>
          if (NonFatal(e2))
            e1.addSuppressed(e2)
          else {
            e2.addSuppressed(e1)
            throw e2
          }
        }

        throw e1
      }
    }
  }
}
