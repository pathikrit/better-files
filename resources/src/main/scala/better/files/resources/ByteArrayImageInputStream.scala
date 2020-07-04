package better.files.resources

import javax.imageio.stream.ImageInputStreamImpl

/**
  * An ImageInputStream implementation that reads from an Array[Byte]
  */
final class ByteArrayImageInputStream(
    val bytes: Array[Byte],
    val bytesOffset: Int,
    val bytesLength: Int
) extends ImageInputStreamImpl {
  def this(bytes: Array[Byte]) = this(bytes, 0, bytes.length)

  final def read(): Int =
    if (streamPos >= bytesLength) -1
    else {
      if (bitOffset > 0) bitOffset = 0
      val res: Int = bytes(streamPos.toInt + bytesOffset) & 0xff
      streamPos += 1
      res
    }

  final def read(b: Array[Byte], off: Int, len: Int): Int =
    if (streamPos >= bytesLength) -1
    else {
      if (bitOffset > 0) bitOffset = 0
      val read: Int = math.min(len, bytesLength - streamPos.toInt)
      System.arraycopy(bytes, streamPos.toInt + bytesOffset, b, off, read)
      streamPos += read
      read
    }

  final override def isCachedMemory(): Boolean = true

  override def length(): Long = bytesLength
}
