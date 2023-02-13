package better.files

import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.GenTraversableOnce

/**
  * An iterator with a close() function that gets called on iterator exhaustion OR any exceptions during iteration
  * Similar in functionality to Geny's self closing generators: https://github.com/com-lihaoyi/geny#self-closing-generators
  * Note:
  *   1) This assumes "exhaustion" on certain operations like find(), exists(), contains(), indexWhere(), forall() etc.
  *      e.g. when find() finds an element we assume iterator exhaustion and thus we trigger close
  *
  *   2) For certain operations that return 2 Iterators e.g. span() and partition(),
  *      to guarantee closing BOTH iterators must be consumed
  *
  *   3) Once close() has been invoked hasNext will always return false and next will throw an IllegalStateException
  */
trait CloseableIterator[+A] extends Iterator[A] with AutoCloseable {
  override def find(p: A => Boolean)                  = evalAndClose(super.find(p))
  override def exists(p: A => Boolean)                = evalAndClose(super.exists(p))
  override def forall(p: A => Boolean)                = evalAndClose(super.forall(p))
  override def indexWhere(p: A => Boolean, from: Int) = evalAndClose(super.indexWhere(p, from))
  override def indexOf[B >: A](elem: B, from: Int)    = evalAndClose(super.indexOf(elem, from))

  // override def takeWhile(p: A => Boolean)                     = closeInTheEnd(super.takeWhile(p))
  override def ++[B >: A](that: => GenTraversableOnce[B])     = closeInTheEnd(super.++(that))
  override protected def sliceIterator(from: Int, until: Int) = closeInTheEnd(super.sliceIterator(from, until))
  override def drop(n: Int) = slice(n, Int.MaxValue) // This is because of a bad implementation in Scala's standard library

  override def zip[B](that: Iterator[B]) =
    that match {
      case other: CloseableIterator[_] => CloseableIterator(super.zip(that), () => other.evalAndClose(this.closeOnce()))
      case _                           => closeInTheEnd(super.zip(that))
    }

  protected val isClosed = new AtomicBoolean(false)
  def closeOnce(): Unit  = if (!isClosed.getAndSet(true)) close()

  /** Close at end of iteration */
  private def closeInTheEnd[T](t: Iterator[T]): Iterator[T] = CloseableIterator(t, closeOnce)

  /** Close this after evaluating f */
  private def evalAndClose[T](f: => T): T =
    try { f }
    finally { closeOnce() }

  /** Close if there is an exception */
  protected def closeIfError[T](f: => T): T =
    try {
      f
    } catch {
      case outer: Throwable =>
        try {
          closeOnce()
        } catch {
          case inner: Throwable =>
            inner.addSuppressed(outer)
            throw inner
        }
        throw outer
    }
}

object CloseableIterator {

  /** Make a closeable iterator given an existing iterator and a close function */
  def apply[A](it: Iterator[A], closeFn: () => Unit): CloseableIterator[A] = new CloseableIterator[A] {
    override def hasNext = !isClosed.get() && {
      val res = closeIfError(it.hasNext)
      if (!res) closeOnce()
      res
    }

    override def next() = {
      require(!isClosed.get(), "Iterator is already closed")
      closeIfError(it.next())
    }
    override def close() = closeFn()
  }
}
