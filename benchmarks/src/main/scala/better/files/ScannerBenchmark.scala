package better.files

import better.files._

object ScannerBenchmark extends App {
  val file = (home / "Downloads" / "tmp.txt").createIfNotExists().clear()
  val n = 1000
  repeat(n) {
    file.appendLine(-n to n mkString " ")
        .appendNewLine()
        .appendLine("hello world " * n)
  }

  def run(scanner: AbstractScanner) = {
    repeat(n) {
      assert(scanner.hasNext)
      List.fill(2*n + 1)(scanner.nextInt())
      scanner.nextLine()
      IndexedSeq.fill(2*n)(scanner.next())
    }
    scanner.close()
  }

  def profile[A](f: => A): (A, Long) = {
    val t = System.nanoTime()
    (f, ((System.nanoTime() - t)/1e6).toLong)
  }

  val (_, t1) = profile(run(new IterableScanner(file.newBufferedReader)))
  val (_, t2) = profile(run(new IteratorScanner(file.newBufferedReader)))
  val (_, t3) = profile(run(new JavaScanner(file.newBufferedReader)))

  println( s"""
    |File = $file
    |Iterable  : $t1 ms
    |Iterator  : $t2 ms
    |Scanner   : $t3 ms
   """.stripMargin
  )
}
