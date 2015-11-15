package better.files

import java.io.{ByteArrayInputStream, InputStream, InputStreamReader, BufferedReader, LineNumberReader}
import java.util.StringTokenizer

import scala.io.Codec
import scala.util.Try

/**
 * Faster, safer and more idiomatic Scala replacement for java.util.Scanner
 * See: http://codeforces.com/blog/entry/7018
 */
class Scanner(reader: LineNumberReader, val delimiter: String, val includeDelimiters: Boolean) {self => //TODO: This API is too complex, work on Scanner v2
  def this(reader: BufferedReader, delimiter: String, includeDelimiters: Boolean) = this(new LineNumberReader(reader), delimiter, includeDelimiters)

  def this(inputStreamReader: InputStreamReader, delimiter: String, includeDelimiters: Boolean) = this(inputStreamReader.buffered, delimiter, includeDelimiters)

  def this(inputStream: InputStream, delimiter: String, includeDelimiters: Boolean)(implicit codec: Codec) = this(inputStream.reader(codec), delimiter, includeDelimiters)

  def this(file: File, delimiter: String, includeDelimiters: Boolean)(implicit codec: Codec) = this(file.newBufferedReader(codec), delimiter, includeDelimiters)

  def this(str: String, delimiter: String = File.Delimiters.default, includeDelimiters: Boolean = false) = this(new ByteArrayInputStream(str.getBytes), delimiter, includeDelimiters)

  private[this] var _tokenizer: Option[PeekableIterator[String]] = None
  private[this] var _nextLine: Option[String] = None
  nextLine()

  private[this] def tokenizer(): Option[PeekableIterator[String]] = _tokenizer.find(_.hasNext) orElse nextLine().flatMap(_ => tokenizer())

  def lineNumber = reader.getLineNumber

  /**
   * This is different from Java's scanner.nextLine
   * The Java one is a misnomer since it actually travels to end of current line and returns that
   * This is a source of much confusion. See: http://stackoverflow.com/questions/5032356/using-scanner-nextline
   * This one actually does return the next line
   *
   * @return returns the next line
   */
  def nextLine(): Option[String] = {
    val line = _nextLine
    _nextLine = Option(reader.readLine())
    _tokenizer = _nextLine map {line => new PeekableIterator(new StringTokenizer(line, delimiter, includeDelimiters))}
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

  def hasMoreTokens: Boolean = tokenizer().exists(_.hasNext)

  def nextToken(): String = tokenizer().get.next()

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
