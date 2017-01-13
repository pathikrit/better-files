package better.files

import java.io.{BufferedReader, StringReader}

object ScannerBenchmark extends App {
  val file = File.newTemporaryFile()
  val n = 1000
  repeat(n) {
    file.appendLine(-n to n mkString " ")
      .appendLine("hello " * n)
      .appendLine("world " * n)
  }
  val scanners: Seq[BufferedReader => AbstractScanner] = Seq(
    new JavaScanner(_),
    new StreamingScanner(_),
    new ArrayBufferScanner(_),
    new CharBufferScanner(_),
    new IteratorScanner(_),
    new IterableScanner(_),
    new StringBuilderScanner(_),
    new BetterFilesScanner(_)
  )

  def test(scanner: AbstractScanner) = {
    val (_, time) = profile(run(scanner))
    scanner.close()
    println(s"${scanner.getClass.getSimpleName}\t: $time ms")
  }

  def run(scanner: AbstractScanner): Unit = repeat(n) {
    assert(scanner.hasNext)
    val ints = List.fill(2 * n + 1)(scanner.nextInt())
    val line = "" //scanner.nextLine()
    val words = IndexedSeq.fill(2 * n)(scanner.next())
    (line, ints, words)
  }

  def profile[A](f: => A): (A, Long) = {
    val t = System.nanoTime()
    (f, ((System.nanoTime() - t) / 1e6).toLong)
  }

  println("Warming up ...")
  scanners foreach { scannerBuilder =>
    val canaryData =
      """
        |10 -23
        |Hello World
        |Hello World
        |19
      """.stripMargin
    val scanner = scannerBuilder(new BufferedReader(new StringReader(canaryData)))
    println(s"Testing ${scanner.getClass.getSimpleName} for correctness")
    assert(scanner.hasNext)
    assert(scanner.nextInt() == 10)
    assert(scanner.nextInt() == -23)
    assert(scanner.next() == "Hello")
    assert(scanner.next() == "World")
    val l = scanner.nextLine()
    assert(l == "Hello World", l)
    assert(scanner.nextInt() == 19)
    //assert(!scanner.hasNext)
  }

  println("Running benchmark ...")
  scanners foreach { scanner => test(scanner(file.newBufferedReader)) }
}
