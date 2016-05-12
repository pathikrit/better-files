package better.files

import shapeless._

import org.scalatest._

class ShapelessScannerSpec extends FlatSpec with Matchers {
  import ShapelessScanner._

  "HList Scanner" should "parse HList" in {
    val in = Scanner("""
      12 Bob True
      13 Mary False
      26 Rick True
    """)

    type Row = Int :: String :: Boolean :: HNil
    val out = Seq.fill(3)(in.next[Row])
    assert(out == Seq(
      12 :: "Bob" :: true :: HNil,
      13 :: "Mary" :: false :: HNil,
      26 :: "Rick" :: true :: HNil
    ))
  }
}
