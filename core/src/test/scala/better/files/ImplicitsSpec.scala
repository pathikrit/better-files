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
}
