package better.files.resources

import org.apache.commons.io.{ByteOrderMark => ApacheByteOrderMark}

object ByteOrderMark extends Enumeration {
  object BOM {
    implicit def apacheBOM(v: BOM): ApacheByteOrderMark = v.apacheBOM
  }

  sealed abstract class BOM(val charsetName: String, protected val apacheBOM: ApacheByteOrderMark) extends super.Val {
    def bytes: Array[Byte] = apacheBOM.getBytes
    def length: Int        = bytes.size
  }

  import scala.language.implicitConversions
  implicit def valueBOM(v: Value): BOM = v.asInstanceOf[BOM]

  case object UTF_8    extends BOM("UTF-8", ApacheByteOrderMark.UTF_8)
  case object UTF_16BE extends BOM("UTF-16BE", ApacheByteOrderMark.UTF_16BE)
  case object UTF_16LE extends BOM("UTF-16LE", ApacheByteOrderMark.UTF_16LE)
  case object UTF_32BE extends BOM("UTF-32BE", ApacheByteOrderMark.UTF_32BE)
  case object UTF_32LE extends BOM("UTF-32LE", ApacheByteOrderMark.UTF_32LE)
}
