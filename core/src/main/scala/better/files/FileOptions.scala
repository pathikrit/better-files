package better.files

import java.nio.file.{LinkOption, StandardWatchEventKinds}
import java.nio.file.attribute.FileAttribute

/**
 * Container default settings for various options
 */
object FileOptions {
  type Attributes = Seq[FileAttribute[_]]
  type Links = Seq[LinkOption]

  def empty[A] = Seq.empty[A]

  val delimiters = " \t\n\r\f"

  val events = Seq(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE)

  val follow: Links = empty
  val noFollow: Links = Seq(LinkOption.NOFOLLOW_LINKS)
}
