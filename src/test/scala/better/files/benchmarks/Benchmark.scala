package better.files.benchmarks

import org.scalatest.funsuite.AnyFunSuite

trait Benchmark extends AnyFunSuite {
  def profile[A](f: => A): (A, Long) = {
    val t = System.nanoTime()
    (f, ((System.nanoTime() - t) / 1e6).toLong)
  }
}
