package better.files

import java.io.{ByteArrayInputStream, InputStream, InputStreamReader, BufferedReader}
import java.util.StringTokenizer

import scala.io.Codec
import scala.util.Try

/**
 * Faster, safer and more idiomatic Scala replacement for java.util.Scanner
 * See: http://codeforces.com/blog/entry/7018
 */
class Scanner(reader: BufferedReader) extends Iterator[String] {

  def this(inputStreamReader: InputStreamReader) = this(inputStreamReader.buffered)

  def this(inputStream: InputStream)(implicit codec: Codec) = this(inputStream.reader(codec))

  def this(file: File)(implicit codec: Codec) = this(file.reader(codec))

  def this(str: String) = this(new ByteArrayInputStream(str.getBytes))

  private[this] var _tokenizer: Option[PeekableStringTokenizer] = None
  private[this] var _nextLine: Option[String] = nextLine()

  private[this] def tokenizer(): Option[PeekableStringTokenizer] = _tokenizer.find(_.hasMoreTokens) orElse nextLine().flatMap(_ => tokenizer())

  def nextLine(): Option[String] = {
    val next = _nextLine
    _nextLine = Option(reader.readLine())
    _tokenizer = _nextLine map {line => new PeekableStringTokenizer(line)}
    next
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

  override def hasNext: Boolean = tokenizer().exists(_.hasMoreTokens)

  override def next(): String = tokenizer().get.nextToken()

  def peek: Option[String] = tokenizer().flatMap(_.peek)

  def peekLine: Option[String] = _nextLine

  def nextInt(): Option[Int]= nextTry(_.toInt)

  def nextLong(): Option[Long] = nextTry(_.toLong)

  def nextDouble(): Option[Double] = nextTry(_.toDouble)

  def nextBoolean(): Option[Boolean] = nextTry(_.toBoolean)

  def nextString(): Option[String] = when(hasNext)(next())

  def nextPattern(pattern: String): Option[String] = nextMatch(_.matches(pattern))

  def nextTry[A](f: String => A): Option[A] = next {x => Try(f(x)).toOption}

  def nextMatch(f: String => Boolean): Option[String] = next {x => when(f(x))(x)}

  def next[A](f: String => Option[A]): Option[A] = for {
    token <- peek
    result <- f(token)
  } yield {
    next()
    result
  }

  def close(): Unit = reader.close()
}

class PeekableStringTokenizer(str: String, delims: String = " \t\n\r\f", returnDelims: Boolean = false) extends StringTokenizer(str, delims, returnDelims) {
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
