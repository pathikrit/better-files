package better.files

import java.io.{InputStream, BufferedReader, LineNumberReader, Reader, StringReader}

import scala.io.Codec

trait Scanner extends Iterator[String] with AutoCloseable {
  val tokens: Iterator[String]

  def lineNumber(): Int

  def next[A: Scannable]: A = implicitly[Scannable[A]].apply(this)

  override def hasNext = tokens.hasNext
  override def next() = tokens.next()
}

/**
 * Faster, safer and more idiomatic Scala replacement for java.util.Scanner
 * See: http://codeforces.com/blog/entry/7018
 */
object Scanner {

  def apply(str: String, delimiter: String = File.Delimiters.default, includeDelimiters: Boolean = false): Scanner = Scanner(new StringReader(str), delimiter, includeDelimiters)

  def apply(reader: Reader, delimiter: String, includeDelimiters: Boolean): Scanner = Scanner(new BufferedReader(reader), delimiter, includeDelimiters)

  def apply(reader: BufferedReader, delimiter: String, includeDelimiters: Boolean): Scanner = Scanner(new LineNumberReader(reader), delimiter, includeDelimiters)

  def apply(inputStream: InputStream, delimiter: String, includeDelimiters: Boolean)(implicit codec: Codec): Scanner = Scanner(inputStream.reader(codec), delimiter, includeDelimiters)

  def apply(file: File, delimiter: String, includeDelimiters: Boolean)(implicit codec: Codec): Scanner = Scanner(file.newBufferedReader(codec), delimiter, includeDelimiters)

  def apply(reader: LineNumberReader, delimiter: String, includeDelimiters: Boolean) = new Scanner {
    override val tokens = reader.tokens(delimiter, includeDelimiters)
    override def lineNumber() = reader.getLineNumber
    override def close() = reader.close()
  }
}

/**
 * Implement this trait to make thing parseable
 */
trait Scannable[A] {
  def apply(scanner: Scanner): A
}

object Scannable {
  def apply[A](f: String => A): Scannable[A] = new Scannable[A] {
    override def apply(scanner: Scanner) = f(scanner.next())
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
