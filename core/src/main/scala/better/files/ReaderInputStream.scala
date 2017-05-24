package better.files

import java.io.{InputStream, Reader}
import java.nio.{ByteBuffer, CharBuffer}
import java.nio.charset.{Charset, CoderResult, CodingErrorAction}

/**
  * Code ported from Java to Scala:
  * https://github.com/apache/commons-io/blob/c0eb48f7e83987c5ed112b82f0d651aff5149ae4/src/main/java/org/apache/commons/io/input/ReaderInputStream.java
  */
class ReaderInputStream(reader: Reader, bufferSize: Int = defaultBufferSize)(implicit charset: Charset = File.defaultCharset) extends InputStream {
  private[this] val encoder = charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE)
  private[this] var endOfInput = false
  private[this] val encoderIn = CharBuffer.allocate(bufferSize)
  private[this] val encoderOut = ByteBuffer.allocate(bufferSize)
  private[this] var lastCoderResult: CoderResult = null

  private def fillBuffer() = {
    if (!endOfInput && (lastCoderResult == null || lastCoderResult.isUnderflow)) {
      encoderIn.compact
      val position = encoderIn.position
      // We don't use Reader#read(CharBuffer) here because it is more efficient
      // to write directly to the underlying char array (the default implementation
      // copies data to a temporary char array).
      val c = reader.read(encoderIn.array, position, encoderIn.remaining)
      if (c == EOF) endOfInput = true
      else encoderIn.position(position + c)
      encoderIn.flip
    }
    encoderOut.compact
    lastCoderResult = encoder.encode(encoderIn, encoderOut, endOfInput)
    encoderOut.flip
  }

  override def read(b: Array[Byte], _off: Int, _len: Int): Int = {
    var len = _len
    var off = _off
    if (b == null) throw new NullPointerException("Byte array must not be null")
    if (len < 0 || off < 0 || (off + len) > b.length) throw new IndexOutOfBoundsException("Array Size=" + b.length + ", offset=" + off + ", length=" + len)
    var read = 0
    if (len == 0) return 0 // Always return 0 if len == 0
    while (len > 0) {
      if (encoderOut.hasRemaining) {
        val c = Math.min(encoderOut.remaining, len)
        encoderOut.get(b, off, c)
        off += c
        len -= c
        read += c
      }
      else {
        fillBuffer()
        if (endOfInput && !encoderOut.hasRemaining) {
          len = 0
        }
      }
    }
    if (read == 0 && endOfInput) EOF else read
  }

  override def read(): Int = {
    while (true) {
      if (encoderOut.hasRemaining) return encoderOut.get & 0xFF
      fillBuffer()
      if (endOfInput && !encoderOut.hasRemaining) return EOF
    }
    -1
  }

  override def close() = reader.close()
}
