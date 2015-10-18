package better.files

import java.io.BufferedReader
import java.util.StringTokenizer

abstract class AbstractScanner(reader: BufferedReader) {
  def hasNext: Boolean
  def next(): String
  def nextInt(): Int
  def nextLine(): String
  def close() = reader.close()
}

class JavaScanner(reader: BufferedReader) extends AbstractScanner(reader) {
  private[this] val scanner = new java.util.Scanner(reader)
  override def hasNext = scanner.hasNext
  override def next() = scanner.next()
  override def nextInt() = scanner.nextInt()
  override def nextLine() = scanner.nextLine()
  override def close() = scanner.close()
}

class IterableScanner(reader: BufferedReader) extends AbstractScanner(reader) with Iterable[String] {
  override def iterator = for {
    line <- Iterator.continually(reader.readLine()).takeWhile(_ != null)
    tokenizer = new StringTokenizer(line)
    tokens <- Iterator.continually(tokenizer).takeWhile(_.hasMoreTokens)
  } yield tokens.nextToken()

  private[this] var current = iterator

  override def hasNext = current.hasNext
  override def next() = current.next()
  override def nextInt() = next().toInt
  override def nextLine() = {
    val line = reader.readLine()
    current = iterator
    line
  }
}

class IteratorScanner(reader: BufferedReader) extends AbstractScanner(reader) with Iterator[String] {
  private[this] var current: Option[StringTokenizer] = None

  @inline private[this] def tokenizer(): Option[StringTokenizer] = current.find(_.hasMoreTokens) orElse {
    Option(reader.readLine()) flatMap {line =>
      current = Some(new StringTokenizer(line))
      tokenizer()
    }
  }
  override def hasNext = tokenizer().exists(_.hasMoreTokens)
  override def next() = tokenizer().get.nextToken()
  override def nextInt() = next().toInt
  override def nextLine() = {
    val line = reader.readLine()
    current = None
    line
  }
}
