package better

import java.io.{File => JFile}
import java.nio.file._
import java.nio.charset.Charset, Charset.defaultCharset
import java.util.stream.{Stream => JStream}

import scala.collection.JavaConversions._
import scala.compat.java8.FunctionConverters._

package object files {
  /**
   * Scala wrapper for java.io.File
   */
  case class File(javaFile: JFile) {
    def javaPath: Path = javaFile.toPath
    def path: String = javaPath.toString

    def name: String = javaFile.getName

    def /(child: String): File = new JFile(javaFile, child)

    def append(lines: String*): File = Files.write(javaPath, lines, defaultCharset(), StandardOpenOption.APPEND, StandardOpenOption.CREATE)
    def <<(line: String): File = append(line)
    val >>: = << _

    def write(text: String): File = Files.write(javaPath, text)
    val overwrite = write _       //TODO: Method alias macro
    val < = write _
    val `>:` = write _

    def contents: File.Contents = Files.readAllBytes(javaPath)
    def contents(charset: Charset = defaultCharset()): String = new String(contents, charset)
    def ! :String = contents()

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
    def isSymLink: Boolean = Files.isSymbolicLink(javaPath)

    def list: Seq[File] = javaFile.listFiles() map File.apply
    def children: Seq[File] = list

    /**
     * Util to glob from this file's path
     * @param ignoreIOExceptions when set to true, any file visit exceptions (e.g. a read or permission error) would be silently ignored
     * @return Set of files that matched
     */
    def glob(pattern: String, syntax: String = "glob", ignoreIOExceptions: Boolean = false): Set[File] = {
      val matcher = FileSystems.getDefault.getPathMatcher(s"$syntax:$pattern")
      Files.walk(javaPath).filter((matcher.matches _).asJava)
    }

    override def toString = path
  }

  object File {
    def newTempDir(prefix: String): File = Files.createTempDirectory(prefix)
    def newTempFile(prefix: String, suffix: String = ""): File = Files.createTempFile(prefix, suffix)

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
    def toFile: File = Paths.get(str)
    def /(child: String): File = toFile / child
  }

  implicit def pathToFile(path: Path): File = path.toFile
  implicit def javaToFile(file: JFile): File = File(file)           //TODO: ISO micro-macros
  implicit def toJavaFile(file: File): JFile = file.javaFile

  private[files] implicit def stringToBytes(s: String): File.Contents = s.getBytes
  private[files] implicit def pathStreamToFileSet(files: JStream[Path]): Set[File] = files.iterator().toSet.map(pathToFile)
  private[files] def when[A](condition: Boolean)(f: => A): Option[A] = if (condition) Some(f) else None
}
