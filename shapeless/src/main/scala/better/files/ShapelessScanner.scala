package better.files

import shapeless._

object ShapelessScanner {
  implicit val hNilScannable: Scannable[HNil] =
    Scannable(_ => HNil)

  implicit def hListScannable[H, T <: HList](implicit h: Lazy[Scannable[H]], t: Scannable[T]): Scannable[H :: T] =
    Scannable(s => h.value(s) :: t(s))

  implicit def genericScannable[A, R](implicit gen: Generic.Aux[A, R], reprScannable: Lazy[Scannable[R]]): Scannable[A] =
    Scannable(s => gen.from(reprScannable.value(s)))
}
