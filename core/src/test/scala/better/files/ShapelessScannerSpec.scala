package better.files

import better.files.Scanner.Read

import shapeless._

import scala.util.Try

object ShapelessScanner {
  implicit val hNilScannable: Scannable[HNil] =
    Scannable(_ => HNil)

  implicit def hListScannable[H, T <: HList](implicit h: Lazy[Scannable[H]], t: Scannable[T]): Scannable[H :: T] =
    Scannable(s => h.value(s) :: t(s))

  implicit def genericScannable[A, R](
      implicit
      gen: Generic.Aux[A, R],
      reprScannable: Lazy[Scannable[R]]
    ): Scannable[A] =
    Scannable(s => gen.from(reprScannable.value(s)))

  implicit val cnilReader: Read[CNil] =
    Read(s => throw new RuntimeException(s"Could not read $s into this coproduct"))

  implicit def coproductReader[H, T <: Coproduct](implicit h: Read[H], t: Read[T]): Read[H :+: T] =
    Read(s => Try(Inl(h(s))).getOrElse(Inr(t(s))))
}

class ShapelessScannerSpec extends CommonSpec {
  import ShapelessScanner._

  val text = """
    12 Bob True
    13 Mary False
    26 Rick True
  """

  "Shapeless Scanner" should "parse HList" in {
    val in = Scanner(text)

    type Row = Int :: String :: Boolean :: HNil
    val out = Seq.fill(3)(in.next[Row])
    assert(
      out == Seq(
        12 :: "Bob" :: true :: HNil,
        13 :: "Mary" :: false :: HNil,
        26 :: "Rick" :: true :: HNil
      )
    )
  }

  "Shapeless Scanner" should "parse case class" in {
    val in = Scanner(text)

    case class Person(id: Int, name: String, isMale: Boolean)
    assert(in.next[Iterator[Person]].map(_.id).sum == 51)
  }
}
