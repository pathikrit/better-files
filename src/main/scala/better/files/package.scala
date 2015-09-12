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

    def name: String = file.getName

    def /(child: String): File = new JFile(file, child)

    def append(lines: String*): File = Files.write(this, lines, defaultCharset(), StandardOpenOption.APPEND, StandardOpenOption.CREATE)
    def <<(line: String): File = append(line)
    val >>: = << _

    def write(text: String): File = Files.write(this, text)
    val overwrite = write _
    val < = write _
    val `>:` = write _

    def contents: File.Contents = Files.readAllBytes(this)
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

    def list: Seq[File] = file.listFiles() map {f => new File(f)}
    def children: Seq[File] = list

    override def toString = path.toString
  }

  object File {
    def apply(path: Path): File = pathToFile(path)

    type Contents = Array[Byte]
  }

  object RegularFile {
    /**
     * @return contents of this file if it is a regular file
     */
    def unapply(file: File): Option[File.Contents] = when(file.isRegularFile)(file.contents)
  }

  object Directory {
    /**
     * @return children of this directory if file a directory
     */
    def unapply(file: File): Option[Seq[File]] = when(file.isDirectory)(file.list)
  }

  object SymbolicLink {
    /**
     * @return target of this symlink if file is a symlink
     */
    def unapply(file: File): Option[File] = file.symLink
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

  private[this] implicit def stringToBytes(s: String): File.Contents = s.getBytes
  private[this] def when[A](condition: Boolean)(f: => A): Option[A] = if (condition) Some(f) else None
}
