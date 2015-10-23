package better.files

import java.nio.file.StandardWatchEventKinds
import java.nio.file.attribute.FileAttribute

/**
 * Sensible defaults that APIs use unless overridden
 */
object Defaults {
  val delimiters = " \t\n\r\f"

  val events = Seq(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE)

  val attributes: FileAttributes = Seq.empty[FileAttribute[_]]
}
