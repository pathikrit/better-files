package better.files

import java.nio.charset.Charset

import scala.util.Random

object EncodingBenchmark extends Benchmark {

  def testWrite(file: File, charset: Charset) = profile {
    for {
      writer <- file.bufferedWriter(charset)
      content <- Iterator.continually(Random.nextString(10000)).take(1000)
    } writer.write(content + "\n")
  }

  def testRead(file: File, charset: Charset) = profile {
    for {
      reader <- file.bufferedReader
      line <- reader.lines().autoClosed
    } line
  }

  def test(charset: Charset) = {
    File.usingTemporaryFile() {file =>
      val (_, w) = testWrite(file, charset)
      println(s"Charset=$charset, write=$w ms")

      val (_, r) = testRead(file, charset)
      println(s"Charset=$charset, read=$r ms")
    }
  }

  val utf8 = Charset.forName("UTF-8")
  test(charset = utf8)
  println("-------------")
  test(charset = UnicodeCharset(utf8))
}
