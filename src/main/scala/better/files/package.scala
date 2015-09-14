package better

import java.io.{File => JFile, IOException}
import java.nio.file._, attribute.{UserPrincipal, FileTime, PosixFilePermission}
import java.nio.charset.Charset, Charset.defaultCharset
import java.security.MessageDigest
import java.time.Instant
import java.util.stream.{Stream => JStream}
import javax.xml.bind.DatatypeConverter

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

    def changeExtensionTo(extension: String): File = if (isRegularFile) renameTo(s"$nameWithoutExtension$extension") else this

    def contentType: Option[String] = Option(Files.probeContentType(javaPath))

    def parent: File = javaFile.getParentFile

    def /(child: String): File = new JFile(javaFile, child)

    def /(f: File => File): File = f(this)

    def createIfNotExists(): File = if (javaFile.exists()) this else Files.createFile(javaPath)

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
    def readLines: Seq[String] = Files.readAllLines(javaPath)

    /**
     * @return checksum of this file in hex format
     */
    def checksum(algorithm: String = "MD5"): String = DatatypeConverter.printHexBinary(MessageDigest.getInstance(algorithm).digest(bytes))

    /**
     * @return Some(target) if this is a symbolic link (to target) else None
     */
    def symLink: Option[File] = when(isSymLink)(Files.readSymbolicLink(javaPath))

    /**
     * @return true if this file (or the file found by following symlink) is a directory
     */
    def isDirectory: Boolean = Files.isDirectory(javaPath)

    /**
     * @return true if this file (or the file found by following symlink) is a regular file
     */
    def isRegularFile: Boolean = Files.isRegularFile(javaPath)

    def isSymLink: Boolean = Files.isSymbolicLink(javaPath)

    def isHidden: Boolean = Files.isHidden(javaPath)

    def list: Files = javaFile.list map File.apply
    def children: Files = list
    def listRecursively(maxDepth: Int = Int.MaxValue): Files = Files.walk(javaPath, maxDepth)

    //TODO: Add def walk(maxDepth: Int): Stream[Path] = that ignores I/O errors and excludes self

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
    def size: Long = listRecursively().map(f => Files.size(f.javaPath)).sum

    def permissions: Set[PosixFilePermission] = Files.getPosixFilePermissions(javaPath).toSet
    def setPermissions(permissions: Set[PosixFilePermission]): File = Files.setPosixFilePermissions(javaPath, permissions)
    def addPermissions(permissions: PosixFilePermission*): File = setPermissions(this.permissions ++ permissions)
    def removePermissions(permissions: PosixFilePermission*): File = setPermissions(this.permissions -- permissions)
    /**
     * test if file has this permission
     */
    def apply(permission: PosixFilePermission): Boolean = permissions(permission)
    def isOwnerReadable   : Boolean = this(PosixFilePermission.OWNER_READ)
    def isOwnerWritable   : Boolean = this(PosixFilePermission.OWNER_WRITE)
    def isOwnerExecutable : Boolean = this(PosixFilePermission.OWNER_EXECUTE)
    def isGroupReadable   : Boolean = this(PosixFilePermission.GROUP_READ)
    def isGroupWritable   : Boolean = this(PosixFilePermission.GROUP_WRITE)
    def isGroupExecutable : Boolean = this(PosixFilePermission.GROUP_EXECUTE)
    def isOtherReadable   : Boolean = this(PosixFilePermission.OTHERS_READ)
    def isOtherWritable   : Boolean = this(PosixFilePermission.OTHERS_WRITE)
    def isOtherExecutable : Boolean = this(PosixFilePermission.OTHERS_EXECUTE)

    def owner: UserPrincipal = Files.getOwner(javaPath)

    def setOwner(owner: String): File = Files.setOwner(javaPath, fileSystem.getUserPrincipalLookupService.lookupPrincipalByName(owner))
    def setGroup(group: String): File = Files.setOwner(javaPath, fileSystem.getUserPrincipalLookupService.lookupPrincipalByGroupName(group))
    val chown = setOwner _
    val chgrp = setGroup _

    /**
     * Similar to the UNIX command touch - create this file if it does not exist and set its last modification time
     */
    def touch(time: Instant = Instant.now()): File = Files.setLastModifiedTime(createIfNotExists().javaPath, FileTime.from(time))

    def lastModifiedTime: Instant = Files.getLastModifiedTime(javaPath).toInstant

    /**
     * Deletes this file or directory
     * @param ignoreIOExceptions If this is set to true, an exception is thrown when delete fails (else it is swallowed)
     */
    def delete(ignoreIOExceptions: Boolean = false): File = {
      try {
        this match {
          case Directory(children) => children.foreach(_.delete(ignoreIOExceptions))
          case _ => javaFile.delete()
        }
      } catch {
        case e: IOException if ignoreIOExceptions => e.printStackTrace() //swallow
      }
      this
    }

    def moveTo(destination: Path, overwrite: Boolean): File = if (overwrite) {
      Files.move(javaPath, destination, StandardCopyOption.REPLACE_EXISTING)
    } else {
      Files.move(javaPath, destination)
    }

    def renameTo(newName: String): File = moveTo(javaPath resolveSibling newName)

    def moveTo(destination: File, overwrite: Boolean = false): File = moveTo(destination.javaPath, overwrite)

    def copyTo(destination: File, overwrite: Boolean = false): File = if (overwrite) {
      Files.copy(javaPath, destination.javaPath, StandardCopyOption.REPLACE_EXISTING)
    } else {
      Files.copy(javaPath, destination.javaPath)
    }

    def linkTo(destination: File, symbolic: Boolean = false): File = if (symbolic) {
      Files.createSymbolicLink(javaPath, destination.javaPath)
    } else {
      Files.createLink(javaPath, destination.javaPath)
    }

    def samePathAs(that: File): Boolean = this.javaPath == that.javaPath

    def sameFileAs(that: File): Boolean = Files.isSameFile(this.javaPath, that.javaPath)

    override def equals(obj: Any) = obj match {
      case file: File => sameFileAs(file)
      case _ => false
    }

    override def hashCode = javaPath.hashCode()

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
    def unapply(file: File): Option[Seq[File]] = when(file.isDirectory)(file.children)
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
