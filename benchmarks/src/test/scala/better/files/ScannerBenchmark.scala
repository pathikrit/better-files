package better.files

import java.io.BufferedReader

object ScannerBenchmark extends App {
  val file = File.newTemp()
  val n = 1000
  repeat(n) {
    file.appendLine(-n to n mkString " ")
        .appendLine("hello " * n)
        .appendLine("world " * n)
  }

  def run(scanner: AbstractScanner): Unit = repeat(n) {
    assert(scanner.hasNext)
    val ints = List.fill(2*n + 1)(scanner.nextInt())
    val line = "" //scanner.nextLine()
    val words = IndexedSeq.fill(2*n)(scanner.next())
    (line, ints, words)
  }

  def profile[A](f: => A): (A, Long) = {
    val t = System.nanoTime()
    (f, ((System.nanoTime() - t)/1e6).toLong)
  }

  def test(scanner: AbstractScanner) = {
    val (_, time) = profile(run(scanner))
    scanner.close()
    println(s"${scanner.getClass.getSimpleName}\t\t\t: $time ms")
  }

  val r5 = test(new CharBufferScanner(file.newBufferedReader))
  val r1 = test(new IterableScanner(file.newBufferedReader))
  val r2 = test(new IteratorScanner(file.newBufferedReader))
  val r3 = test(new JavaScanner(file.newBufferedReader))
  val r4 = test(new StreamingScanner(file.newBufferedReader))

  /*
  assert(r1 == r2)
  assert(r2 == r3)
  assert(r3 == r4)
  assert(r4 == r5)*/
}
