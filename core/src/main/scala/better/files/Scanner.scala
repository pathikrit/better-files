package better.files

import java.io.{InputStream, BufferedReader, LineNumberReader, Reader, StringReader}

import scala.io.Codec

trait Scanner extends Iterator[String] with AutoCloseable {
  def lineNumber(): Int

  def next[A: Scannable]: A = implicitly[Scannable[A]].apply(this)

  def tillDelimiter(delimiter: String): String

  def tillEndOfLine() = tillDelimiter("\n\r")

  def nonEmptyLines: Iterator[String] = produce(tillEndOfLine()).till(hasNext)
}

/**
 * Faster, safer and more idiomatic Scala replacement for java.util.Scanner
 * See: http://codeforces.com/blog/entry/7018
 */
object Scanner {

  def apply(str: String)(implicit config: Config = Config.default): Scanner = Scanner(new StringReader(str))(config)

  def apply(reader: Reader)(implicit config: Config): Scanner = Scanner(reader.buffered)(config)

  def apply(reader: BufferedReader)(implicit config: Config): Scanner = Scanner(new LineNumberReader(reader))(config)

  def apply(inputStream: InputStream)(implicit config: Config): Scanner = Scanner(inputStream.reader(config.codec))(config)

  def apply(reader: LineNumberReader)(implicit config: Config): Scanner = new Scanner {
    private[this] val tokenizers = reader.tokenizers(config).buffered
    private[this] def tokenizer() = {
      while(tokenizers.nonEmpty && !tokenizers.head.hasMoreTokens) tokenizers.next()
      when(tokenizers.nonEmpty)(tokenizers.head)
    }
    override def lineNumber() = reader.getLineNumber
    override def tillDelimiter(delimiter: String) = tokenizer().get.nextToken(delimiter)
    override def next() = tokenizer().get.nextToken()
    override def hasNext = tokenizer().nonEmpty
    override def close() = reader.close()
  }

  val stdIn = Scanner(System.in)(Config.default)

  /**
   * Use this to configure your Scanner
   *
   * @param delimiter
   * @param includeDelimiters
   */
  case class Config(delimiter: String, includeDelimiters: Boolean)(implicit val codec: Codec)
  object Config {
    val default = Config(delimiter = " \t\n\r\f", includeDelimiters = false)
  }
}

/**
 * Implement this trait to make thing parseable
 */
trait Scannable[A] {
  def apply(scanner: Scanner): A
  def map[B](f: A => B): Scannable[B] = Scannable(apply _ andThen f)
  def +[B](that: Scannable[B]): Scannable[(A, B)] = Scannable(s => this(s) -> that(s))
}

object Scannable {
  def apply[A](f: Scanner => A): Scannable[A] = new Scannable[A] {
    override def apply(scanner: Scanner) = f(scanner)
  }
  implicit val stringScanner      : Scannable[String]     = Scannable(_.next())
  implicit val boolScanner        : Scannable[Boolean]    = stringScanner.map(_.toBoolean)
  implicit val byteScanner        : Scannable[Byte]       = stringScanner.map(_.toByte)
  implicit val shortScanner       : Scannable[Short]      = stringScanner.map(_.toShort)
  implicit val intScanner         : Scannable[Int]        = stringScanner.map(_.toInt)
  implicit val longScanner        : Scannable[Long]       = stringScanner.map(_.toLong)
  implicit val bigIntScanner      : Scannable[BigInt]     = stringScanner.map(BigInt(_))
  implicit val floatScanner       : Scannable[Float]      = stringScanner.map(_.toFloat)
  implicit val doubleScanner      : Scannable[Double]     = stringScanner.map(_.toDouble)
  implicit val bigDecimalScanner  : Scannable[BigDecimal] = stringScanner.map(BigDecimal(_))
}
