package better.files

import scala.collection.mutable

/**
 * An util to convert an iterator to one that supports peeking
 *
 * @param iterator
 * @param lookAhead
 * @tparam A
 */
class PeekableIterator[A](iterator: Iterator[A], lookAhead: Int = 1) extends Iterator[A] {
  private[this] val buffer = mutable.Queue.empty[A] ++= (iterator take lookAhead).ensuring(lookAhead > 0)

  def peek: Option[A] = buffer.headOption

  def peek(n: Int): Iterable[A] = buffer take n

  override def hasNext = buffer.nonEmpty

  override def next = (if(iterator.hasNext) buffer += iterator.next() else buffer).dequeue()
}
