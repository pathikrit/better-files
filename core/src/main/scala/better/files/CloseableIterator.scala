package better.files

import java.util.concurrent.atomic.AtomicBoolean

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
  override def find(p: A => Boolean)      = evalAndClose(super.find(p))
  override def exists(p: A => Boolean)    = evalAndClose(super.exists(p))
  override def forall(p: A => Boolean)    = evalAndClose(super.forall(p))
  override def takeWhile(p: A => Boolean) = closeInTheEnd(super.takeWhile(p))

  private[files] val isClosed          = new AtomicBoolean(false)
  private[files] def closeOnce(): Unit = if (!isClosed.getAndSet(true)) close()

  /** Close at end of iteration */
  private[files] def closeInTheEnd[T](t: Iterator[T]): Iterator[T] = CloseableIterator(t, closeOnce)

  /** Close this after evaluating f */
  private[files] def evalAndClose[T](f: => T): T =
    try { f }
    finally { closeOnce() }

  /** Close if there is an exception */
  private[files] def closeIfError[T](f: => T): T =
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

  /** Returns a non closing version of this iterator
    * This means partial operations like find() and drop() will NOT close the iterator
    *
    * @param closeInTheEnd If this is true, it will ONLY close the iterator in the end when it has no more elements (default behaviour)
    *                      and not on partial evaluations like find() and take() etc.
    *                      If this is false, iterator will be ALWAYS left open i.e. close() will be NEVER invoked
    *                      and is up to user to close any underlying resources
    */
  def nonClosing(closeInTheEnd: Boolean): Iterator[A]
}

object CloseableIterator {

  /** Make a closeable iterator given an existing iterator and a close function */
  def apply[A](it: Iterator[A], closeFn: () => Unit): CloseableIterator[A] = new CloseableIteratorCompat[A] { self =>
    override def hasNext = !isClosed.get() && {
      val res = closeIfError(it.hasNext)
      if (!res) closeOnce()
      res
    }

    override def next() = {
      if (isClosed.get()) throw new IllegalStateException("Iterator is already closed")
      closeIfError(it.next())
    }

    override def close() = closeFn()

    override def nonClosing(closeInTheEnd: Boolean) = it match {
      case c: CloseableIterator[A] => c.nonClosing(closeInTheEnd)
      case _ if !closeInTheEnd     => it
      case _ =>
        new Iterator[A] {
          override def hasNext = self.hasNext
          override def next()  = self.next()
        }
    }
  }

  def from[A](resource: AutoCloseable)(f: resource.type => Iterator[A]): CloseableIterator[A] =
    CloseableIterator(f(resource), resource.close)
}
