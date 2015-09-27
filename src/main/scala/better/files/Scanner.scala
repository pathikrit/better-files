package better.files

import java.io.{ByteArrayInputStream, InputStream, InputStreamReader, BufferedReader}
import java.util.StringTokenizer

import scala.io.Codec
import scala.util.Try

/**
 * Faster, safer and more idiomatic Scala replacement for java.util.Scanner
 * See: http://codeforces.com/blog/entry/7018
 */
class Scanner(reader: BufferedReader, val delimiter: String, val includeDelimiters: Boolean) extends Iterator[String] {

  def this(inputStreamReader: InputStreamReader, delimiter: String, includeDelimiters: Boolean) = this(inputStreamReader.buffered, delimiter, includeDelimiters)

  def this(inputStream: InputStream, delimiter: String, includeDelimiters: Boolean)(implicit codec: Codec) = this(inputStream.reader(codec), delimiter, includeDelimiters)

  def this(file: File, delimiter: String = Scanner.defaultDelimiter, includeDelimiters: Boolean = false)(implicit codec: Codec) = this(file.reader(codec), delimiter, includeDelimiters)

  def this(str: String, delimiter: String, includeDelimiters: Boolean) = this(new ByteArrayInputStream(str.getBytes), delimiter, includeDelimiters)

  private[this] var _tokenizer: Option[PeekableStringTokenizer] = None
  private[this] var _nextLine: Option[String] = nextLine()

  private[this] def tokenizer(): Option[PeekableStringTokenizer] = _tokenizer.find(_.hasMoreTokens) orElse nextLine().flatMap(_ => tokenizer())

  def nextLine(): Option[String] = {
    val line = _nextLine
    _nextLine = Option(reader.readLine())
    _tokenizer = _nextLine map {line => new PeekableStringTokenizer(line, delimiter, includeDelimiters)}
    line
  }

  /**
   * Skip l lines and then skip t tokens
   * @param lines
   * @param tokens
   * @return this
   */
  def skip(lines: Int, tokens: Int = 0): Scanner = {
    repeat(lines + 1)(nextLine())
    repeat(tokens)(next())
    this
  }

  def skip(pattern: String): Scanner = {
    nextPattern(pattern)
    this
  }

  override def hasNext: Boolean = tokenizer().exists(_.hasMoreTokens)

  override def next(): String = tokenizer().get.nextToken()

  def peek: Option[String] = tokenizer().flatMap(_.peek)

  def peekLine: Option[String] = _nextLine

  def nextByte(): Option[Byte] = nextTry(_.toByte)

  def nextInt(): Option[Int]= nextTry(_.toInt)  //TODO: radix support

  def nextLong(): Option[Long] = nextTry(_.toLong)

  def nextDouble(): Option[Double] = nextTry(_.toDouble)

  def nextFloat(): Option[Float] = nextTry(_.toFloat)

  def nextShort(): Option[Short] = nextTry(_.toShort)

  def nextBoolean(): Option[Boolean] = nextTry(_.toBoolean)

  def nextString(): Option[String] = when(hasNext)(next())

  def nextBigInt(): Option[BigInt] = nextTry(BigInt(_))

  def nextBigDecimal(): Option[BigDecimal] = nextTry(BigDecimal(_))

  def nextPattern(pattern: String): Option[String] = nextMatch(_.matches(pattern))

  @inline def nextTry[A](f: String => A): Option[A] = nextSuccess {x => Try(f(x))}

  @inline def nextSuccess[A](f: String => Try[A]): Option[A] = next {x => f(x).toOption}

  @inline def nextMatch(f: String => Boolean): Option[String] = next {x => when(f(x))(x)}

  @inline def next[A](f: String => Option[A]): Option[A] = for {
    token <- peek
    result <- f(token)
  } yield {
    next()
    result
  }

  def close(): Unit = reader.close()
}

class PeekableStringTokenizer(str: String, delim: String = Scanner.defaultDelimiter, returnDelims: Boolean = false) extends StringTokenizer(str, delim, returnDelims) {
  private[this] var current: Option[String] = None
  nextToken()

  def peek: Option[String] = current

  override def hasMoreTokens = current.nonEmpty

  override def nextToken() = {
    val prev = current
    current = when(super.hasMoreTokens)(super.nextToken())
    prev.orNull
  }
}

object Scanner {
  val defaultDelimiter = " \t\n\r\f"
}
