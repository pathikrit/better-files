package better.files

import java.io.{ByteArrayInputStream, InputStream, InputStreamReader, BufferedReader}
import java.util.StringTokenizer

import scala.io.Codec

/**
 * Scala implementation of a faster java.util.Scanner
 * See: http://codeforces.com/blog/entry/7018
 */
class Scanner(reader: BufferedReader) extends Iterator[String] {
  //TODO: Implement hasNextBoolean etc. see java.io.Scanner methods

  def this(inputStreamReader: InputStreamReader) = this(inputStreamReader.buffered)

  def this(inputStream: InputStream)(implicit codec: Codec) = this(inputStream.reader(codec))

  def this(file: File)(implicit codec: Codec) = this(file.reader(codec))

  def this(str: String) = this(new ByteArrayInputStream(str.getBytes))

  private[this] var tokenizer: Option[StringTokenizer] = None

  private[this] def nextTokenizer(): Option[StringTokenizer] = tokenizer.find(_.hasMoreTokens) orElse {
    Option(reader.readLine()) flatMap {line =>
      tokenizer = Some(new StringTokenizer(line))
      nextTokenizer()
    }
  }

  def nextLine(): String = {
    tokenizer = None
    reader.readLine()
  }

  /**
   * Skip l lines and then skip t tokens
   * @param lines
   * @param tokens
   * @return this
   */
  def skip(lines: Int = 1, tokens: Int = 0): Scanner = {
    repeat(lines + 1)(nextLine())
    repeat(tokens)(next())
    this
  }

  override def hasNext: Boolean = nextTokenizer().exists(_.hasMoreTokens)

  override def next(): String = nextTokenizer().get.nextToken()

  def nextInt(): Int = next().toInt

  def nextLong(): Long = next().toLong

  def nextDouble(): Double = next().toDouble

  def nextBoolean(): Boolean = next().toBoolean

  def close(): Unit = reader.close()
}
