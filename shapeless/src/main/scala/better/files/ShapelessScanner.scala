package better.files

import shapeless._

object ShapelessScanner {
  implicit val hNilScannable: Scannable[HNil] =
    Scannable(_ => HNil)

  implicit def hListScannable[H, T <: HList](implicit h: Scannable[H], t: Scannable[T]): Scannable[H :: T] =
    Scannable(s => h(s) :: t(s))
}
