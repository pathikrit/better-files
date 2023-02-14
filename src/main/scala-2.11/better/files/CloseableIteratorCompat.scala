package better.files

import scala.collection.GenTraversableOnce

trait CloseableIteratorCompat[+A] extends CloseableIterator[A] { self =>
  override def indexWhere(p: A => Boolean)                = evalAndClose(super.indexWhere(p))
  override def indexOf[B >: A](elem: B)                   = evalAndClose(super.indexOf(elem))
  override def ++[B >: A](that: => GenTraversableOnce[B]) = closeInTheEnd(super.++(that))

  override def drop(n: Int) = slice(n, Int.MaxValue)

  override def slice(from: Int, until: Int) = closeInTheEnd(new Iterator[A] {
    var i = 0

    override def hasNext = {
      while (i < from && self.hasNext) i += 1
      i < until && self.hasNext
    }

    override def next() = {
      i += 1
      self.next()
    }
  })

  override def zip[B](that: Iterator[B]) =
    that match {
      case other: CloseableIterator[_] => CloseableIterator(super.zip(that), () => other.evalAndClose(this.closeOnce()))
      case _                           => closeInTheEnd(super.zip(that))
    }
}
