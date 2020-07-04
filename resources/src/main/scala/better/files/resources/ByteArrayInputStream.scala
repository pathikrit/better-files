package better.files.resources

import java.io.{InputStream, IOException, SequenceInputStream}
import java.nio.{ByteBuffer, InvalidMarkException}
import scala.collection.JavaConverters._

object ByteBufferInputStream {

  /**
    * This takes combined multiple ByteBuffers into a single InputStream by
    * wrapping them in ByteBufferInputStreams and then using a
    * java.io.SequenceInputStream to combine them.
    *
    * NOTE: This calls duplicate() on the ByteBuffer to prevent modifying the original
    */
  def apply(bufs: Seq[ByteBuffer]): InputStream = {
    if (bufs.size == 1) new ByteBufferInputStream(bufs.head.duplicate())
    else {
      val bios = bufs
        .map { buf: ByteBuffer =>
          new ByteBufferInputStream(buf.duplicate())
        }
        .iterator
        .asJavaEnumeration

      new SequenceInputStream(bios)
    }
  }
}

/**
  * A Simple InputStream wrapper around a ByteBuffer
  */
final class ByteBufferInputStream(buf: ByteBuffer) extends InputStream {
  override def markSupported(): Boolean = true

  override def mark(readlimit: Int): Unit = buf.mark()

  override def reset(): Unit =
    try {
      buf.reset()
    } catch {
      // InputStream specifies that we throw an IOException "if the stream has
      // not been marked or if the mark has been invalidated"
      case ex: InvalidMarkException => throw new IOException(ex)
    }

  def read(): Int = {
    if (!buf.hasRemaining()) return -1
    buf.get() & 0xff
  }

  override def read(bytes: Array[Byte], off: Int, len: Int): Int = {
    if (!buf.hasRemaining()) return -1

    val lenToRead = math.min(len, buf.remaining())
    buf.get(bytes, off, lenToRead)
    lenToRead
  }
}
