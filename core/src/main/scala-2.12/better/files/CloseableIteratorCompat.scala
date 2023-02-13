package better.files

import scala.collection.GenTraversableOnce

trait CloseableIteratorCompat[+A] extends CloseableIterator[A] {
  override protected def sliceIterator(from: Int, until: Int) = closeInTheEnd(super.sliceIterator(from, until))
  override def indexWhere(p: A => Boolean, from: Int)         = evalAndClose(super.indexWhere(p, from))
  override def indexOf[B >: A](elem: B, from: Int)            = evalAndClose(super.indexOf(elem, from))
  override def ++[B >: A](that: => GenTraversableOnce[B])     = closeInTheEnd(super.++(that))

  override def drop(n: Int) = slice(n, Int.MaxValue)

  override def zip[B](that: Iterator[B]) =
    that match {
      case other: CloseableIterator[_] => CloseableIterator(super.zip(that), () => other.evalAndClose(this.closeOnce()))
      case _                           => closeInTheEnd(super.zip(that))
    }
}
