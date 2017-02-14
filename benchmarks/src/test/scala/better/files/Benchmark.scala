package better.files

abstract class Benchmark extends App {
  def profile[A](f: => A): (A, Long) = {
    val t = System.nanoTime()
    (f, ((System.nanoTime() - t) / 1e6).toLong)
  }
}
