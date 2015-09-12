package better

import java.io.{File => JFile}
import java.nio.charset.Charset, Charset.defaultCharset
import java.nio.file.{Path => JPath, StandardOpenOption, Files, Paths}

import scala.collection.JavaConversions._

package object files {
  /**
   * Scala wrapper for java.io.File
   */
  case class File(javaFile: JFile) {
    def javaPath: JPath = javaFile.toPath
    def path: String = javaPath.toString

    def name: String = javaFile.getName

    def /(child: String): File = new JFile(javaFile, child)

    def append(lines: String*): File = Files.write(javaPath, lines, defaultCharset(), StandardOpenOption.APPEND, StandardOpenOption.CREATE)
    def <<(line: String): File = append(line)
    val >>: = << _

    def write(text: String): File = Files.write(javaPath, text)
    val overwrite = write _
    val < = write _
    val `>:` = write _

    def contents: File.Contents = Files.readAllBytes(javaPath)
    def contents(charset: Charset = defaultCharset()): String = new String(contents, charset)

    /**
     * @return Some(target) if this is a symbolic link (to target) else None
     */
    def symLink: Option[File] = when(Files.isSymbolicLink(javaPath))(Files.readSymbolicLink(javaPath))

    /**
     * @return true if this file (or the file found by following symlink) is a directory
     */
    def isDirectory: Boolean = Files.isDirectory(javaPath)

    /**
     * @return true if this file (or the file found by following symlink) is a regular file
     */
    def isRegularFile: Boolean = Files.isRegularFile(javaPath)

    def list: Seq[File] = javaFile.listFiles() map File.apply
    def children: Seq[File] = list

    override def toString = path
  }

  object File {
    def apply(path: String): File = path.toFile

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

  def root: File = JFile.listRoots().head
  def home: File = sys.props("user.home").toFile

  implicit class StringInterpolations(sc: StringContext) {
    def file(args: Any*): File = sc.s(args: _*).toFile
  }

  implicit class StringOps(str: String) {
    def toFile: File = lift(Paths.get(str))
    def /(child: String): File = toFile / child
  }

  implicit def lift(path: JPath): File = path.toFile
  implicit def lift(file: JFile): File = File(file)
  implicit def toJavaFile(file: File): JFile = file.javaFile

  private[this] implicit def stringToBytes(s: String): File.Contents = s.getBytes
  private[this] def when[A](condition: Boolean)(f: => A): Option[A] = if (condition) Some(f) else None
}
