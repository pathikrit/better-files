package better.files

import scala.util.Try

class CloseableIteratorSpec extends CommonSpec {
  "iterators" can "handle completions" in {
    def check[A](name: Symbol, f: Iterator[Int] => A) = withClue(name) {
      var closed     = false
      def iterator() = (1 to 10).toIterator
      val it = CloseableIterator(
        iterator(),
        () => {
          assert(!closed, "Already closed!")
          closed = true
        }
      )
      f(it) match {
        case result: Iterator[_] => // Test when we make new iterators e.g. .map()
          assert(!closed, "We just made an iterator, closed must not be called yet")
          (1 to 4).foreach(_ => it.hasNext) // Call hasNext bunch of times to make sure we call close() atmost once
          assert(!closed)
          val _ = result.asInstanceOf[Iterator[A]].size // Trigger onComplete

        case (l: Iterator[_], r: Iterator[_]) => // Test .partition(), .span(), .duplicate() etc.
          assert(!closed, "Creating 2 iterators must not trigger close")
          assert(Try(l.isEmpty).isSuccess)
          assert(!closed, "Atleast l or r must be completed to trigger close")
          assert(Try(r.isEmpty).isSuccess)
          assert(!closed, "Atleast l or r must be completed to trigger close")
          val _ = l.size + r.size // Triggers completion

        case result =>
          (1 to 4).foreach(_ => it.hasNext) // Call hasNext bunch of times to make sure we call close() atmost once
          assert(result == f(iterator()), "Different result found over native iterator")
      }
      assert(closed)
    }

    check('zipWithIndex, _.zipWithIndex)
    check('takeLess, _.take(3))
    check('takeMore, _.take(15))
    check('dropLess, _.drop(3))
    check('dropMore, _.drop(15))
    check('sliceLess, _.slice(3, 7))
    check('sliceMore, _.slice(3, 70))
    check('map, _.map(_.toString))
    check('flatMapIterator, _.flatMap(i => Iterator(i, 2 * i)))
    check('flatMapEmpty, _.flatMap(_ => Iterator.empty))
    check('flatMapList, _.flatMap(i => List(i, 2 * i)))
    check('filterNone, _.filter(_ < 0))
    check('filterSome, _.filter(_ % 2 == 0))
    check('collectNone, _.collect({ case i if i < 0 => i }))
    check('collectSome, _.collect({ case i if i % 2 == 0 => i }))
    check('scanLeft, _.scanLeft(0)(_ + _))
    check('scanRight, _.scanRight(0)(_ + _).toList) // scanRight does close because it needs to go to end
    check('takeNone, _.takeWhile(_ < 0))
    check('takeSome, _.takeWhile(_ < 5))
    check('dropNone, _.dropWhile(_ < 0))
    check('dropSome, _.dropWhile(_ < 5))
    check('partition, _.partition(_ % 2 == 0))
    check('partitionSwap, _.partition(_ % 2 == 0).swap)
    check('span, _.span(_ > 5))
    check('spanEmpty, _.span(_ < 0))
    check('padTo, _.padTo(100, 0))
    check('padToLess, _.padTo(0, 0))
    check('foreach, _.foreach(_ => ()))
    check('findNone, _.find(_ < 0))
    check('findSome, _.find(_ == 3))
    check('existsFalse, _.exists(_ < 0))
    check('existsTrue, _.exists(_ == 5))
    check('containsFalse, _.contains(5))
    check('containsTrue, _.contains(-5))
    check('indexOfFalse, _.indexOf(-5))
    check('indexOfTrue, _.indexOf(5))
    check('indexWhereFalse, _.indexWhere(_ < 0))
    check('indexWhereTrue, _.indexWhere(_ > 5))
    check('forAllFalse, _.forall(_ < 0))
    check('forAllTrue, _.forall(_ < 100))
    check('seq, _.seq)
    check('buffered, _.buffered)
    check('zipLarge, _.zip(Iterator.continually(0)))
    check('zipSmall, _.zip(Iterator(1, 0)))
    check('zipWithIndex, _.zipWithIndex)
    check('zipEmpty, _.zip(Iterator.empty))
    check('groupedLarge, _.grouped(100))
    check('groupedSmall, _.grouped(2))
    check('slidingSmall, _.sliding(2, 3))
    check('slidingLarge, _.sliding(20, 3))
    check('slidingLarge2, _.sliding(1, 23))
    check('toList, _.toList)
    check('size, _.size)
    check('duplicate, _.duplicate)
    check('patch, _.patch(from = 3, patchElems = (1 to 5).iterator, replaced = 3))
    check('appendAndTakeSome, it => (it ++ Iterator(11, 12, 13)).take(3))
    check('appendAndTakeMore, it => (it ++ Iterator(11, 12, 13)).take(11))
  }

  // TODO: Check for multi iterator
}
