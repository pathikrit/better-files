package better.files.resources

import java.io.OutputStream
import java.nio.{ByteBuffer, MappedByteBuffer}

/**
  * A Simple OutputStream wrapper around a ByteBuffer
  */
final class ByteBufferOutputStream(buf: ByteBuffer) extends OutputStream {
  def write(b: Int): Unit = buf.put(b.toByte)

  override def write(bytes: Array[Byte], off: Int, len: Int): Unit = buf.put(bytes, off, len)

  /**
    * If this is a MappedByteBuffer then force() is called to cause changes to be written to disk
    */
  override def flush(): Unit =
    buf match {
      case mapped: MappedByteBuffer => mapped.force()
      case _                        => // Do nothing
    }
}
