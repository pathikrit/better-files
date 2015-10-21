package better.files

import java.io.BufferedReader

/**
 * Base interface to test
 */
abstract class AbstractScanner(protected[this] val reader: BufferedReader) {
  def hasNext: Boolean
  def next(): String
  def nextInt() = next().toInt
  def nextLine() = reader.readLine()
  def close() = reader.close()
}

/**
 * Based on java.util.Scanner
 */
class JavaScanner(reader: BufferedReader) extends AbstractScanner(reader) {
  private[this] val scanner = new java.util.Scanner(reader)
  override def hasNext = scanner.hasNext
  override def next() = scanner.next()
  override def nextInt() = scanner.nextInt()
  override def nextLine() = {
    scanner.nextLine()
    scanner.nextLine()
  }
  override def close() = scanner.close()
}

/**
 * Based on StringTokenizer + resetting the iterator
 */
class IterableScanner(reader: BufferedReader) extends AbstractScanner(reader) with Iterable[String] {
  import scala.collection.JavaConversions._
  private[this] val lineIterator: Iterator[String] = reader.lines().iterator()
  override def iterator = for {
    line <- lineIterator
    tokenizer = new java.util.StringTokenizer(line)
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

/**
 * Based on a mutating var StringTokenizer
 */
class IteratorScanner(reader: BufferedReader) extends AbstractScanner(reader) with Iterator[String] {
  import java.util.StringTokenizer
  private[this] var current: Option[StringTokenizer] = None

  @inline private[this] def tokenizer(): Option[StringTokenizer] = current.find(_.hasMoreTokens) orElse {
    Option(reader.readLine()) flatMap {line =>
      current = Some(new StringTokenizer(line))
      tokenizer()
    }
  }
  override def hasNext = tokenizer().exists(_.hasMoreTokens)
  override def next() = tokenizer().get.nextToken()
  override def nextLine() = {
    val line = super.nextLine()
    current = None
    line
  }
}

/**
 * Based on java.io.StreamTokenizer
 */
class StreamingScanner(reader: BufferedReader) extends AbstractScanner(reader) with Iterator[String] {
  import java.io.StreamTokenizer
  private[this] val in = new StreamTokenizer(reader)

  override def hasNext = in.ttype != StreamTokenizer.TT_EOF
  override def next() = {
    in.nextToken()
    in.sval
  }
  def nextDouble() = {
    in.nextToken()
    in.nval
  }
  override def nextInt() = nextDouble().toInt
}

/**
 * Based on a reusable StringBuilder
 */
class StringBuilderScanner(reader: BufferedReader) extends AbstractScanner(reader) with Iterator[String] {
  private[this] val charIterator = Iterator.continually(reader.read()).takeWhile(_ != -1).map(_.toChar)
  private[this] val buffer = new StringBuilder()
  override def hasNext = charIterator.hasNext
  final override def next() = {
    buffer.clear()
    while(buffer.isEmpty && hasNext) {
      charIterator.takeWhile(c => !c.isWhitespace).foreach(buffer += _)
    }
    buffer.toString()
  }
}
