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

  def nextBoolean(): Option[Boolean] = nextTry(_.toBoolean)

  def nextByte(radix: Int = 10): Option[Byte] = nextTry(java.lang.Byte.parseByte(_, radix))

  def nextShort(radix: Int = 10): Option[Short] = nextTry(java.lang.Short.parseShort(_, radix))

  def nextInt(radix: Int = 10): Option[Int]= nextTry(java.lang.Integer.parseInt(_, radix))

  def nextLong(radix: Int = 10): Option[Long] = nextTry(java.lang.Long.parseLong(_, radix))

  def nextBigInt(radix: Int = 10): Option[BigInt] = nextTry(BigInt(_, radix))

  def nextFloat(): Option[Float] = nextTry(_.toFloat)

  def nextDouble(): Option[Double] = nextTry(_.toDouble)

  def nextBigDecimal(): Option[BigDecimal] = nextTry(BigDecimal(_))

  def nextString(): Option[String] = when(hasNext)(next())

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
