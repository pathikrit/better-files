package better.files

import java.io.{ByteArrayInputStream, InputStream, InputStreamReader, BufferedReader, LineNumberReader}

import scala.io.Codec

trait Scanner extends Iterable[String] with AutoCloseable {
  def lineNumber: Int

  def next[A: Scannable]: A = implicitly[Scannable[A]].apply(this)
}

/**
 * Faster, safer and more idiomatic Scala replacement for java.util.Scanner
 * See: http://codeforces.com/blog/entry/7018
 */
object Scanner {

  def apply(reader: BufferedReader, delimiter: String, includeDelimiters: Boolean): Scanner = Scanner(new LineNumberReader(reader), delimiter, includeDelimiters)

  def apply(inputStreamReader: InputStreamReader, delimiter: String, includeDelimiters: Boolean): Scanner = Scanner(inputStreamReader.buffered, delimiter, includeDelimiters)

  def apply(inputStream: InputStream, delimiter: String, includeDelimiters: Boolean)(implicit codec: Codec): Scanner = Scanner(inputStream.reader(codec), delimiter, includeDelimiters)

  def apply(file: File, delimiter: String, includeDelimiters: Boolean)(implicit codec: Codec): Scanner = Scanner(file.newBufferedReader(codec), delimiter, includeDelimiters)

  def apply(str: String, delimiter: String = File.Delimiters.default, includeDelimiters: Boolean = false): Scanner = Scanner(new ByteArrayInputStream(str.getBytes), delimiter, includeDelimiters)

  def apply(reader: LineNumberReader, delimiter: String, includeDelimiters: Boolean) = new Scanner {
    override val iterator = reader.tokens(delimiter, includeDelimiters )
    override def lineNumber = reader.getLineNumber
    override def close(): Unit = reader.close()
  }
}

/**
 * Implement this trait to make thing scannable
 */
trait Scannable[A] {
  def apply(scanner: Scanner): A
}

object Scannable {
  def apply[A](f: String => A): Scannable[A] = new Scannable[A] {
    override def apply(scanner: Scanner) = f(scanner.iterator.next())
  }
  implicit val boolScanner: Scannable[Boolean] = Scannable(_.toBoolean)
  implicit val byteScanner: Scannable[Byte] = Scannable(_.toByte)
  implicit val shortScanner: Scannable[Short] = Scannable(_.toShort)
  implicit val intScanner: Scannable[Int]= Scannable(_.toInt)
  implicit val longScanner: Scannable[Long] = Scannable(_.toLong)
  implicit val bigIntScanner: Scannable[BigInt] = Scannable(BigInt(_))
  implicit val floatScanner: Scannable[Float] = Scannable(_.toFloat)
  implicit val doubleScanner: Scannable[Double] = Scannable(_.toDouble)
  implicit val bigDecimalScanner: Scannable[BigDecimal] = Scannable(BigDecimal(_))
  implicit val stringScanner: Scannable[String] = Scannable(identity)
}
