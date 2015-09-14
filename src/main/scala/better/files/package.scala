package better

import java.io.{File => JFile}
import java.nio.file._
import java.nio.charset.Charset, Charset.defaultCharset
import java.nio.file.attribute.PosixFilePermission
import java.util.stream.{Stream => JStream}

import scala.collection.JavaConversions._
import scala.compat.java8.FunctionConverters._
import scala.io.Source

package object files {
  /**
   * Scala wrapper for java.io.File
   */
  case class File(javaFile: JFile) {
    def javaPath: Path = javaFile.toPath
    def path: String = javaPath.toString

    def name: String = javaFile.getName
    def nameWithoutExtension: String = if (hasExtension) name.substring(0, name lastIndexOf ".") else name

    /**
     * @return extension (including the dot) of this file if it is a regular file and has an extension, else None
     */
    def extension: Option[String] = when(hasExtension)(name substring (name indexOf "."))
    def hasExtension: Boolean = isRegularFile && (name contains ".")

    def parent: File = javaFile.getParentFile

    def /(child: String): File = new JFile(javaFile, child)

    def /(f: File => File): File = f(this)

    def appendLines(lines: String*): File = Files.write(javaPath, lines, defaultCharset(), StandardOpenOption.APPEND, StandardOpenOption.CREATE)
    def <<(line: String): File = appendLines(line)
    val >>: = << _
    def appendNewLine: File = appendLines("")

    def write(bytes: File.Contents): File = Files.write(javaPath, bytes)
    def write(text: String): File = Files.write(javaPath, text)
    val overwrite = write _       //TODO: Method alias macro
    val < = write _
    val `>:` = write _

    def bytes: File.Contents = Files.readAllBytes(javaPath)
    def read(charset: Charset = defaultCharset()): String = new String(bytes, charset)
    def contents: String = read()
    def `!`:String = contents

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

    def list: Files = javaFile.listFiles() map File.apply
    def children: Files = list
    def listRecursively: Files = Files.walk(javaPath)

    /**
     * Util to glob from this file's path
     * @param ignoreIOExceptions when set to true, any file visit exceptions (e.g. a read or permission error) would be silently ignored
     * @return Set of files that matched
     */
    def glob(pattern: String, syntax: String = "glob", ignoreIOExceptions: Boolean = false): Files = {
      val matcher = fileSystem.getPathMatcher(s"$syntax:$pattern")
      Files.walk(javaPath).filter((matcher.matches _).asJava)
    }

    def fileSystem: FileSystem = javaPath.getFileSystem

    /**
     * @return file size (for directories, return size of the directory) in bytes
     */
    def size: Long = listRecursively.map(f => Files.size(f.javaPath)).sum

    def permissions: Set[PosixFilePermission] = Files.getPosixFilePermissions(javaPath).toSet

    def setPermissions(permissions: Set[PosixFilePermission]): File = Files.setPosixFilePermissions(javaPath, permissions)

    /**
     * Add file permissions
     */
    def +=(permissions: PosixFilePermission*): File = setPermissions(this.permissions ++ permissions)

    /**
     * Remove file permissions
     */
    def -=(permissions: PosixFilePermission*): File = setPermissions(this.permissions -- permissions)


    override def toString = path
  }

  object File {
    def newTempDir(prefix: String): File = Files.createTempDirectory(prefix)
    def newTemp(prefix: String, suffix: String = ""): File = Files.createTempFile(prefix, suffix)

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

  type Files = Seq[File]

  def root: File = FileSystems.getDefault.getRootDirectories.head
  def home: File = sys.props("user.home").toFile
  val `..`: File => File = _.parent
  val  `.`: File => File = identity

  implicit class StringInterpolations(sc: StringContext) {
    def file(args: Any*): File = value(args).toFile
    def resource(args: Any*): Source = Source.fromInputStream(getClass.getResourceAsStream(value(args)))
    private[this] def value(args: Seq[Any]) = sc.s(args: _*)
  }

  implicit class StringOps(str: String) {
    def toFile: File = Paths.get(str)
    def /(child: String): File = toFile / child
  }

  implicit def pathToFile(path: Path): File = path.toFile
  implicit def javaToFile(file: JFile): File = File(file)           //TODO: ISO micro-macros
  implicit def toJavaFile(file: File): JFile = file.javaFile

  private[files] implicit def stringToBytes(s: String): File.Contents = s.getBytes
  private[files] implicit def pathStreamToFiles(files: JStream[Path]): Files = files.iterator().toSeq.map(pathToFile)
  private[files] def when[A](condition: Boolean)(f: => A): Option[A] = if (condition) Some(f) else None
}
