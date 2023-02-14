package better.files

import scala.collection.IterableOnce

private[files] trait CloseableIteratorCompat[+A] extends CloseableIterator[A] {
  override protected def sliceIterator(from: Int, until: Int) = closeInTheEnd(super.sliceIterator(from, until))
  override def indexWhere(p: A => Boolean, from: Int)         = evalAndClose(super.indexWhere(p, from))
  override def indexOf[B >: A](elem: B, from: Int)            = evalAndClose(super.indexOf(elem, from))
  override def concat[B >: A](xs: => IterableOnce[B])         = closeInTheEnd(super.concat(xs))

  override def zip[B](that: IterableOnce[B]) =
    that match {
      case other: CloseableIterator[_] => CloseableIterator(super.zip(that), () => other.evalAndClose(this.closeOnce()))
      case _                           => closeInTheEnd(super.zip(that))
    }
}
