package better.files

import scala.collection.mutable

/**
 * An util to convert an iterator to one that supports peeking
 *
 * @param iterator
 * @tparam A
 */
class PeekableIterator[A](iterator: Iterator[A]) extends Iterator[A] {
  private[this] var bufferSize = 1
  private[this] val buffer = mutable.Queue.empty[A] ++= (iterator take bufferSize)

  def peek: Option[A] = buffer.headOption

  def peek(n: Int): Iterable[A] = {
    if (n > bufferSize) {
      buffer ++= (iterator take (n - bufferSize))
      bufferSize = n
    }
    buffer take n
  }

  override def hasNext = buffer.nonEmpty

  override def next = (buffer ++= (iterator take 1)).dequeue()
}
