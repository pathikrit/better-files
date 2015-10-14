package better.files

import java.io.{ByteArrayInputStream, InputStream, InputStreamReader, BufferedReader}
import java.util.StringTokenizer

import scala.io.Codec
import scala.util.Try

/**
 * Faster, safer and more idiomatic Scala replacement for java.util.Scanner
 * See: http://codeforces.com/blog/entry/7018
 */
class Scanner(reader: BufferedReader, val delimiter: String, val includeDelimiters: Boolean) {self =>

  def this(inputStreamReader: InputStreamReader, delimiter: String, includeDelimiters: Boolean) = this(inputStreamReader.buffered, delimiter, includeDelimiters)

  def this(inputStream: InputStream, delimiter: String, includeDelimiters: Boolean)(implicit codec: Codec) = this(inputStream.reader(codec), delimiter, includeDelimiters)

  def this(file: File, delimiter: String, includeDelimiters: Boolean)(implicit codec: Codec) = this(file.newBufferedReader(codec), delimiter, includeDelimiters)

  def this(str: String, delimiter: String = Scanner.defaultDelimiter, includeDelimiters: Boolean = false) = this(new ByteArrayInputStream(str.getBytes), delimiter, includeDelimiters)

  private[this] var _tokenizer: Option[PeekableStringTokenizer] = None
  private[this] var _nextLine: Option[String] = None
  nextLine()

  private[this] def tokenizer(): Option[PeekableStringTokenizer] = _tokenizer.find(_.hasMoreTokens) orElse nextLine().flatMap(_ => tokenizer())

  def nextLine(): Option[String] = {
    val line = _nextLine
    _nextLine = Option(reader.readLine())
    _tokenizer = _nextLine map {line => new PeekableStringTokenizer(line, delimiter, includeDelimiters)}
    line
  }

  def skipLines(lines: Int, startAtBeginningOfNextLine: Boolean = true): Scanner = returning(this) {
    repeat(lines + (if (startAtBeginningOfNextLine) 1 else 0))(nextLine())
  }

  def skipLine(): Scanner = skipLines(lines = 0, startAtBeginningOfNextLine = true)

  def skip(tokens: Int): Scanner = returning(this)(repeat(tokens)(nextToken()))

  def skip(pattern: String): Scanner = returning(this)(next(pattern))

  def peekToken: Option[String] = tokenizer().flatMap(_.peek)

  def peekLine: Option[String] = _nextLine

  def hasMoreTokens: Boolean = tokenizer().exists(_.hasMoreTokens)

  def nextToken(): String = tokenizer().get.nextToken()

  def next(pattern: String)(): Option[String] = next[String]()(Scannable.fromPattern(pattern))

  def next[A: Scannable](): Option[A] = scan[A](moveToNextToken = true)

  def peek[A: Scannable]: Option[A] = scan[A](moveToNextToken = false)

  def nextTry[A](f: String => A): Option[A] = next[A]()(Scannable(f))

  def peekTry[A](f: String => A): Option[A] = peek[A](Scannable(f))

  def nextSuccess[A](f: String => Try[A]): Option[A] = next[A]()(Scannable.fromTry(f))

  def peekSuccess[A](f: String => Try[A]): Option[A] = peek[A](Scannable.fromTry(f))

  def nextDefined[A](f: String => Option[A]): Option[A] = next[A]()(Scannable.from(f))

  def peekDefined[A](f: String => Option[A]): Option[A] = next[A]()(Scannable.from(f))

  def nextMatch(f: String => Boolean): Option[String] = nextDefined[String] {x => when(f(x))(x)}

  def scan[A: Scannable](moveToNextToken: Boolean): Option[A] = for {
    token <- peekToken
    scanner = implicitly[Scannable[A]]
    result <- scanner.scan(token)(this)
  } yield {
    if (moveToNextToken) nextToken()
    result
  }

  def iterator[A: Scannable]: Iterator[A] = new Iterator[A] {
    override def hasNext = peek[A].nonEmpty
    override def next() = self.next[A]().get
  }

  def close(): Unit = reader.close()
}

class PeekableStringTokenizer(s: String, delimiter: String = Scanner.defaultDelimiter, includeDelimiters: Boolean = false) extends StringTokenizer(s, delimiter, includeDelimiters) {
  private[this] var next: Option[String] = None
  nextToken()

  def peek: Option[String] = next

  override def hasMoreTokens = next.nonEmpty

  override def nextToken() = {
    val token = next
    next = when(super.hasMoreTokens)(super.nextToken())
    token.orNull
  }
}

object Scanner {
  val defaultDelimiter = " \t\n\r\f"
}

/**
 * Implement this trait to make thing scannable
 */
trait Scannable[A] {
  def scan(token: String)(implicit context: Scanner): Option[A]
}

object Scannable {
  def apply[A](f: String => A): Scannable[A] = fromTry((x: String) => Try(f(x)))

  def fromTry[A](f: String => Try[A]): Scannable[A] = from((x: String) => f(x).toOption)

  def from[A](f: String => Option[A]): Scannable[A] = new Scannable[A] {
    override def scan(token: String)(implicit context: Scanner) = f(token)
  }

  implicit val scanBool: Scannable[Boolean] = Scannable(_.toBoolean)
  implicit val scanByte: Scannable[Byte] = Scannable(_.toByte)
  implicit val scanShort: Scannable[Short] = Scannable(_.toShort)
  implicit val scanInt: Scannable[Int]= Scannable(_.toInt)
  implicit val scanLong: Scannable[Long] = Scannable(_.toLong)
  implicit val scanBigInt: Scannable[BigInt] = Scannable(BigInt(_))
  implicit val scanFloat: Scannable[Float] = Scannable(_.toFloat)
  implicit val scanDouble: Scannable[Double] = Scannable(_.toDouble)
  implicit val scanBigDecimal: Scannable[BigDecimal] = Scannable(BigDecimal(_))
  implicit val scanString: Scannable[String] = Scannable(identity)

  def fromPattern(pattern: String) = Scannable.from((x: String) => when(x matches pattern)(x))
}
