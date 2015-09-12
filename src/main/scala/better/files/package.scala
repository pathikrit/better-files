package better

import java.io.{File => JFile}
import java.nio.charset.Charset, Charset.defaultCharset
import java.nio.file.{Path => JPath, StandardOpenOption, Files, Paths}

import scala.collection.JavaConversions._

package object files {
  /**
   * Scala wrapper for java.io.File
   */
  implicit class File(val file: JFile) {
    def path: Path = file.toPath

    def /(child: String): File = new JFile(file, child)

    def append(lines: String*): File = Files.write(this, lines, defaultCharset(), StandardOpenOption.APPEND, StandardOpenOption.CREATE)
    def <<(line: String): File = append(line)
    val >>: = << _

    def write(text: String): File = Files.write(this, text)
    val overwrite = write _
    val < = write _
    val ->: = write _

    def contents: Array[Byte] = Files.readAllBytes(this)
    def contents(charset: Charset = defaultCharset()): String = new String(contents, charset)

    /**
     * @return Some(target) if this is a symbolic link (to target) else None
     */
    def symLink: Option[File] = when(Files.isSymbolicLink(this))(Files.readSymbolicLink(this))

    /**
     * @return true if this file (or the file found by following symlink) is a directory
     */
    def isDirectory: Boolean = Files.isDirectory(this)

    /**
     * @return true if this file (or the file found by following symlink) is a regular file
     */
    def isRegularFile: Boolean = Files.isRegularFile(this)

    override def toString = path.toString
  }

  object File {
    def apply(path: Path): File = pathToFile(path)
  }

  /**
   * A trait to capture various file types
   * Note: A file may not fall into any of these types e.g. UNIX pipes, sockets, devices etc
   * @see https://en.wikipedia.org/wiki/Unix_file_types
   */
  sealed trait FileType

  case class RegularFile(contents: Array[Byte]) extends FileType
  object RegularFile {
    def unapply(file: File): Option[RegularFile] = when(file.isRegularFile)(RegularFile(file.contents))
  }

  case class Directory(children: Seq[File]) extends FileType
  object Directory {
    def unapply(file: File): Option[Directory] = when(file.isDirectory)(Directory(file.listFiles().map(f => new File(f))))
  }

  case class SymbolicLink(to: File) extends FileType
  object SymbolicLink {
    def unapply(file: File): Option[SymbolicLink] = file.symLink map SymbolicLink.apply
  }

  /**
   * Scala wrapper for java.nio.file.Path
   */
  implicit class Path(val path: JPath) {
    def file: File = path.toFile

    def /(name: String): Path = path.resolve(name)

    override def toString = path.toAbsolutePath.toString
  }

  def root: Path = JFile.listRoots().head.toPath
  def home: Path = sys.props("user.home")

  implicit class StringInterpolations(sc: StringContext) {
    def file(args: Any*): File = pathToFile(sc.s(args: _*))
  }

  implicit def stringToPath(str: String): Path = Paths.get(str)

  implicit def pathToFile(path: Path): File = path.file
  implicit def fileToPath(file: File): Path = file.path

  implicit def pathToJavaPath(path: Path): JPath = path.path
  implicit def fileToJavaFile(file: File): JFile = file.file

  implicit def fileToJavaPath(file: File): JPath = fileToPath(file)
  implicit def javaPathToFile(path: JPath): File = pathToFile(path)

  private[this] implicit def stringToBytes(s: String): Array[Byte] = s.getBytes
  private[this] def when[A](condition: Boolean)(f: => A): Option[A] = if (condition) Some(f) else None
}
