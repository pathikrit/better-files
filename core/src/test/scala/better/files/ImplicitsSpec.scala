package better.files

import org.scalatest._

class ImplicitsSpec extends CommonSpec {
  "streams" can "be partitioned" in {
    File.usingTemporaryDirectory() { dir =>
      (dir / "1.csv").touch()
      (dir / "2.csv").touch()
      (dir / "3.txt").touch()
      val (csv, other) = dir.listRecursively().partition(_.extension.contains(".csv"))
      assert(csv.size == 2)
      assert(other.size == 1)
    }
  }

  "iterators" can "handle completions" in {
    def mustNotInvoke[A](name: Symbol, f: Iterator[Int] => Iterator[A]) = withClue(name) {
      var counter = 0
      val it      = (1 to 10).toIterator.onComplete({ counter += 1 })
      val result  = f(it)
      assert(counter === 0)
      // Call hasNext bunch of times to make sure we call onComplete atmost once
      assert(it.hasNext === it.hasNext)
      assert(it.hasNext === it.hasNext)
      assert(counter === 0)
      val _ = result.size //Trigger onComplete
      assert(counter == 1)
    }

    def mustInvoke[A](name: Symbol, f: Iterator[Int] => A) = withClue(name) {
      var counter = 0
      val it      = (1 to 10).toIterator.onComplete({ counter += 1 })
      val _       = f(it)
      // Call hasNext bunch of times to make sure we call onComplete atmost once
      assert(it.hasNext === it.hasNext)
      assert(it.hasNext === it.hasNext)
      assert(counter === 1)
    }

    mustNotInvoke('map, _.map(_.toString))
    mustNotInvoke('zipWithIndex, _.zipWithIndex)

    mustInvoke('toList, _.toList)
    mustInvoke('size, _.size)
    mustInvoke('takeLess, _.take(3))
    mustInvoke('takeMore, _.take(15))
    mustInvoke('findNone, _.find(_ < 0))
    mustInvoke('findSome, _.find(_ == 3))
    mustInvoke('appendAndTakeSome, it => (it ++ Iterator(11, 12, 13)).take(3))
    mustInvoke('appendAndTakeMore, it => (it ++ Iterator(11, 12, 13)).take(11))
  }
}
