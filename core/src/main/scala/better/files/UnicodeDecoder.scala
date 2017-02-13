package better.files

import java.nio.charset.{Charset, CharsetDecoder, CoderResult}
import java.nio.{ByteBuffer, CharBuffer}

/**
  * A Unicode decoder that uses the Unicode byte-order marker (BOM) to auto-detect the encoding
  * (if none detected, falls back on the defaultCharset). This also gets around a bug in the JDK
  * (http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4508058) where BOM is not consumed for UTF-8
  * by always consuming the BOM. See: https://github.com/pathikrit/better-files/issues/107
  *
  * @param defaultCharset Use this charset if no known byte-order marker is detected
  */
class UnicodeDecoder(defaultCharset: Charset) extends CharsetDecoder(null, 1, 1) {
  import UnicodeDecoder._

  private var inferredCharset: Option[Charset] = None

  override def decodeLoop(in: ByteBuffer, out: CharBuffer) =
    decode(
      in = in,
      out = out,
      candidates = bomTable.keys.toList,
      header = ByteBuffer.allocate(4).mark().asInstanceOf[ByteBuffer]
    )

  @annotation.tailrec
  private def decode(in: ByteBuffer, out: CharBuffer, candidates: List[Charset], header: ByteBuffer): CoderResult = {
    if (isCharsetDetected) {
      val decoder = detectedCharset().newDecoder()
      val hasMore = in.remaining() > 0
      if (header.remaining() > 0) decoder.decode(header, out, !hasMore)
      decoder.decode(in, out, true)
    } else if (candidates.isEmpty || in.remaining() <= 0) {
      inferredCharset = Some(defaultCharset)
      header.reset()
      decode(in, out, candidates, header)
    } else {
      val byte = in.get()
      val idx = header.position()
      header.put(byte)
      val newCandidates = candidates filter {charset =>
        val bom = bomTable(charset)
        bom.isDefinedAt(idx) && bom(idx) == byte
      }
      newCandidates match {
        case charset :: Nil if bomTable(charset).length == idx + 1 =>
          inferredCharset = Some(charset)
          header.limit(idx)
        case _ =>
      }
      decode(in, out, newCandidates, header)
    }
  }

  override def isCharsetDetected = inferredCharset.isDefined

  override def isAutoDetecting = true

  override def implReset() = inferredCharset = None

  override def detectedCharset() = inferredCharset.getOrElse(throw new IllegalStateException("Insufficient bytes read to determine charset"))
}

object UnicodeDecoder {
  private[UnicodeDecoder] val bomTable: Map[Charset, IndexedSeq[Byte]] = Map(
    "UTF-8"    -> IndexedSeq(0xEF, 0xBB, 0xBF),
    "UTF-32BE" -> IndexedSeq(0x00, 0x00, 0xFE, 0xFF),
    "UTF-32LE" -> IndexedSeq(0xFF, 0xFE, 0x00, 0x00),
    "UTF-16BE" -> IndexedSeq(0xFE, 0xFF),
    "UTF-16LE" -> IndexedSeq(0xFF, 0xFE)
  ).collect{case (charset, bytes) if Charset.isSupported(charset) => Charset.forName(charset) -> bytes.map(_.toByte)}
   .ensuring(_.nonEmpty, "No unicode charset detected")

  def apply(charset: Charset): Charset = {
    import scala.collection.JavaConverters._
    new Charset(charset.name(), charset.aliases().asScala.toArray) {
      override def newDecoder() = new UnicodeDecoder(charset)
      override def newEncoder() = charset.newEncoder()
      override def contains(cs: Charset) = charset.contains(cs)
    }
  }
}