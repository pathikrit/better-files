package better.files

import java.io.OutputStream

/**
  * Write to multiple outputstreams at once
  *
  * @param outs
  */
class TeeOutputStream(outs: OutputStream*) extends OutputStream {
  override def write(b: Int) = outs.foreach(_.write(b))
  override def flush() = outs.foreach(_.flush())
  override def write(b: Array[Byte]) = outs.foreach(_.write(b))
  override def write(b: Array[Byte], off: Int, len: Int) = outs.foreach(_.write(b, off, len))
  override def close() = outs.foreach(_.close())
}
