package better.files

import java.io.BufferedReader

import scala.annotation.tailrec

/**
 * Base interface to test
 */
abstract class AbstractScanner(reader: BufferedReader) {
  def hasNext: Boolean
  def next(): String
  def nextInt(): Int
  def nextLine(): String
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
  override def iterator = for {
    line <- Iterator.continually(reader.readLine()).takeWhile(_ != null)
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
  override def nextInt() = next().toInt
  override def nextLine() = {
    val line = reader.readLine()
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
  override def nextLine() = ???
}

/**
 * Hand built custom scanner using a Char buffer
 */
class CharBufferScanner(reader: BufferedReader) extends AbstractScanner(reader) {
  private[this] var buffer = Array.ofDim[Char](1<<10)
  override def hasNext = true
  @tailrec final override def next() = {
    var pos = 0
    var c = reader.read().toChar
    while(!c.isWhitespace) {
      if (pos == buffer.length) {
        buffer = java.util.Arrays.copyOf(buffer, 2*pos)
      }
      buffer(pos) = c
      pos += 1
      c = reader.read().toChar
    }
    if (pos == 0) next() else String.copyValueOf(buffer, 0, pos)
  }
  override def nextInt() = next().toInt
  override def nextLine() = ???
}
