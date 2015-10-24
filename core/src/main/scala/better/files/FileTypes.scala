package better.files

import scala.io.BufferedSource

sealed trait FileType

object FileTypes {
  case object RegularFile extends FileType {
    /**
     * @return contents of this file if it is a regular file
     */
    def unapply(file: File): Option[BufferedSource] = when(file.isRegularFile)(file.newBufferedSource)
  }

  case object Directory extends FileType {
    /**
     * @return children of this directory if file a directory
     */
    def unapply(file: File): Option[Files] = when(file.isDirectory)(file.children)
  }

  case object SymbolicLink extends FileType {
    /**
     * @return target of this symlink if file is a symlink
     */
    def unapply(file: File): Option[File] = file.symbolicLink
  }
}
