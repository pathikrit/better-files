package better.files

import java.io.{IOException, OutputStream, Writer}
import java.nio.{ByteBuffer, CharBuffer}
import java.nio.charset.{Charset, CharsetDecoder, CoderResult, CodingErrorAction}

//TODO: Rename writeImmediately to flushImmediately

/**
  * Code ported from Java to Scala:
  * https://github.com/apache/commons-io/blob/d357d9d563c4a34fa2ab3cdc68221c851a9de4f5/src/main/java/org/apache/commons/io/output/WriterOutputStream.java
  */
class WriterOutputStream(writer: Writer, decoder: CharsetDecoder, bufferSize: Int, writeImmediately: Boolean) extends OutputStream {

  /**
    * CharBuffer used as output for the decoder
    */
  private[this] val decoderOut = CharBuffer.allocate(bufferSize)

  /**
    * ByteBuffer used as output for the decoder. This buffer can be small
    * as it is only used to transfer data from the decoder to the buffer provided by the caller.
    */
  private[this] val decoderIn = ByteBuffer.allocate(128)

  def this(writer: Writer, bufferSize: Int = defaultBufferSize, writeImmediately: Boolean = false)(implicit charset: Charset = File.defaultCharset) =
    this(writer, charset.newDecoder.onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith("?"), bufferSize, writeImmediately)

  override def write(b: Array[Byte], _off: Int, _len: Int) = {
    var off = _off
    var len = _len
    while (len > 0) {
      val c = Math.min(len, decoderIn.remaining)
      decoderIn.put(b, off, c)
      processInput(false)
      len -= c
      off += c
    }
    val _ = if (writeImmediately) flushOutput()
  }

  override def write(b: Int) = write(Array[Byte](b.toByte), 0, 1)

  override def flush() = {
    flushOutput()
    writer.flush()
  }

  override def close() = {
    processInput(true)
    flushOutput()
    writer.close()
  }

  private[this] def processInput(endOfInput: Boolean) = { // Prepare decoderIn for reading
    decoderIn.flip
    var coderResult: CoderResult = null
    do {
      coderResult = decoder.decode(decoderIn, decoderOut, endOfInput)
      if (coderResult.isOverflow) flushOutput()
      else if (coderResult.isUnderflow) {

      } else { // The decoder is configured to replace malformed input and unmappable characters,
        // so we should not get here.
        throw new IOException("Unexpected coder result")
      }
    } while (!coderResult.isUnderflow)
    // Discard the bytes that have been read
    decoderIn.compact
  }

  private[this] def flushOutput() = {
    if (decoderOut.position > 0) {
      writer.write(decoderOut.array, 0, decoderOut.position)
      decoderOut.rewind
    }
  }
}