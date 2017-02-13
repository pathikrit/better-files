package better.files

import java.nio.charset._
import java.nio.{BufferOverflowException, ByteBuffer, CharBuffer}

import scala.collection.JavaConverters._

/**
  * A Unicode charset that handles byte-order markers
  *
  * @param underlyingCharset Use this charset if no known byte-order marker is detected; use this for encoding too
  * @param writeByteOrderMarkers If set, write BOMs while encoding
  */
class UnicodeCharset(underlyingCharset: Charset, writeByteOrderMarkers: Boolean)
  extends Charset(underlyingCharset.name(), underlyingCharset.aliases().asScala.toArray) {
  override def newDecoder() = new UnicodeDecoder(underlyingCharset)
  override def newEncoder() = if (writeByteOrderMarkers) new BomEncoder(underlyingCharset) else underlyingCharset.newEncoder()
  override def contains(cs: Charset) = underlyingCharset.contains(cs)
}

/**
  * A Unicode decoder that uses the Unicode byte-order marker (BOM) to auto-detect the encoding
  * (if none detected, falls back on the defaultCharset). This also gets around a bug in the JDK
  * (http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4508058) where BOM is not consumed for UTF-8.
  * See: https://github.com/pathikrit/better-files/issues/107
  *
  * @param defaultCharset Use this charset if no known byte-order marker is detected
  */
class UnicodeDecoder(defaultCharset: Charset) extends CharsetDecoder(defaultCharset, 1, 1) {
  import UnicodeCharset.bomTable

  private[this] var inferredCharset: Option[Charset] = None

  @annotation.tailrec
  private[this] def decode(in: ByteBuffer, out: CharBuffer, candidates: Set[Charset] = Set.empty): CoderResult = {
    if (isCharsetDetected) {
      detectedCharset().newDecoder().decode(in, out, true)
    } else if (candidates.isEmpty || in.remaining() <= 0) {
      inferredCharset = Some(defaultCharset)
      in.rewind()
      decode(in, out)
    } else if (candidates.forall(c => bomTable(c).length == in.position())) {
      inferredCharset = candidates.headOption.ensuring(candidates.size == 1, "Ambiguous BOMs found")
      decode(in, out)
    } else {
      val idx = in.position()
      val byte = in.get()
      def isPossible(charset: Charset) = bomTable(charset).lift(idx).contains(byte)
      decode(in, out, candidates.filter(isPossible))
    }
  }

  override def decodeLoop(in: ByteBuffer, out: CharBuffer) = decode(in = in, out = out, candidates = bomTable.keySet)

  override def isCharsetDetected = inferredCharset.isDefined

  override def isAutoDetecting = true

  override def implReset() = inferredCharset = None

  override def detectedCharset() = inferredCharset.getOrElse(throw new IllegalStateException("Insufficient bytes read to determine charset"))
}

/**
  * Encoder that writes the BOM for this charset
  * @param charset
  */
class BomEncoder(charset: Charset) extends CharsetEncoder(charset, 1, 1) {
  private[this] val bom = UnicodeCharset.bomTable.getOrElse(charset, throw new IllegalArgumentException(s"$charset does not support BOMs")).toArray
  private[this] var isBomWritten = false
  private[this] val defaultEncoder = charset.newEncoder()

  override def encodeLoop(in: CharBuffer, out: ByteBuffer) = {
    if (!isBomWritten) {
      try {
        out.put(bom)
        isBomWritten = true
      } catch {
        case _: BufferOverflowException => CoderResult.OVERFLOW
      }
    }
    defaultEncoder.encode(in, out, true)
  }

  override def malformedInputAction() = defaultEncoder.malformedInputAction()
  //override def implReset() = {defaultEncoder.implReset(); isBomWritten = false}
  override def unmappableCharacterAction() = defaultEncoder.unmappableCharacterAction()
  //override def isLegalReplacement(repl: Array[Byte]) = defaultEncoder.isLegalReplacement(repl)
  override def canEncode(c: Char) = defaultEncoder.canEncode(c)
  override def canEncode(cs: CharSequence) = defaultEncoder.canEncode(cs)
}

object UnicodeCharset {
  private[files] val bomTable: Map[Charset, IndexedSeq[Byte]] = Map(
    "UTF-8"    -> IndexedSeq(0xEF, 0xBB, 0xBF),
    "UTF-32BE" -> IndexedSeq(0x00, 0x00, 0xFE, 0xFF),
    "UTF-32LE" -> IndexedSeq(0xFF, 0xFE, 0x00, 0x00),
    "UTF-16BE" -> IndexedSeq(0xFE, 0xFF),
    "UTF-16LE" -> IndexedSeq(0xFF, 0xFE)
  ).collect{case (charset, bytes) if Charset.isSupported(charset) => Charset.forName(charset) -> bytes.map(_.toByte)}
   .ensuring(_.nonEmpty, "No unicode charset detected")

  def apply(charset: Charset, writeByteOrderMarkers: Boolean = false): Charset =
    if (bomTable.contains(charset)) new UnicodeCharset(charset, writeByteOrderMarkers) else charset
}