package better.files.resources

import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object ByteBufferUtil {

  /**
    * Like FileChannel.map except assumes you want the whole file and returns
    * multiple MappedByteBuffers if the file is larger than Integer.MAX_VALUE
    */
  def map(raf: RandomAccessFile, mode: FileChannel.MapMode): Vector[MappedByteBuffer] = map(raf.getChannel(), mode)

  /**
    * Like FileChannel.map except assumes you want the whole file and returns
    * multiple MappedByteBuffers if the file is larger than Integer.MAX_VALUE
    */
  def map(ch: FileChannel, mode: FileChannel.MapMode): Vector[MappedByteBuffer] = {
    val totalSize: Long = ch.size()

    if (totalSize == 0) return Vector.empty
    if (totalSize <= Int.MaxValue) return Vector(ch.map(mode, 0, totalSize))

    val builder = Vector.newBuilder[MappedByteBuffer]

    var start: Long = 0
    var size: Long  = totalSize

    while (size > 0) {
      val thisSize: Long = math.min(Int.MaxValue.toLong, size)
      builder += ch.map(mode, start, thisSize)
      start += thisSize
      size -= thisSize
    }

    builder.result
  }
}
