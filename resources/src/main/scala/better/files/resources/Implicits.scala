package better.files.resources

import rich._
import java.io.InputStream

trait Implicits extends better.files.Implicits {
  implicit def toRichInputStream(is: InputStream): RichInputStream = new RichInputStream(is)
}
