package better

import java.io.{File => JFile, FileSystem => JFileSystem, _}
import java.nio.channels.FileChannel
import java.nio.file._, attribute._
import java.nio.charset.Charset
import java.security.MessageDigest
import java.time.Instant
import java.util.function.Predicate
import java.util.stream.{Stream => JStream}
import javax.xml.bind.DatatypeConverter

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.io.{BufferedSource, Codec, Source}
import scala.util.Properties

package object files {
  /**
   * Scala wrapper for java.io.File
   */
  case class File(file: JFile) {

    def toJava: JFile = file

    def path: Path = file.toPath

    def fullPath: String = path.toString

    def name: String = file.getName     //TODO: alias as fileName

    def nameWithoutExtension: String = if (hasExtension) name.substring(0, name lastIndexOf ".") else name

    /**
     * @return extension (including the dot) of this file if it is a regular file and has an extension, else None
     */
    def extension: Option[String] = when(hasExtension)(name substring (name indexOf "."))

    def hasExtension: Boolean = isRegularFile && (name contains ".")

    /**
     * Changes the file-extension by renaming this file; if file does not have an extension, it adds the extension
     * Example usage file"foo.java".changeExtensionTo(".scala")
     */
    def changeExtensionTo(extension: String): File = if (isRegularFile) renameTo(s"$nameWithoutExtension$extension") else this

    def contentType: Option[String] = Option(Files.probeContentType(path))

    def parent: File = file.getParentFile.toScala

    def /(child: String): File = new JFile(file, child).toScala

    def /(f: File => File): File = f(this)

    //def createChild(name: String): File = Files.createFile(mkdirs().path, name)

    def createIfNotExists(): File = if (file.exists()) this else {parent.mkdirs(); Files.createFile(path)}

    def exists: Boolean = file.exists()

    def content(implicit codec: Codec): BufferedSource = Source.fromFile(file)(codec)
    def source(implicit codec: Codec): BufferedSource = content(codec)

    def bytes: Iterator[Byte] = {
      val stream = in
      Iterator.continually(stream.read()).takeWhile(-1 !=).map(_.toByte)  //TODO: close the stream
    }

    def mkdirs(): File = Files.createDirectories(path)

    def chars(implicit codec: Codec): Iterator[Char] = content(codec)

    def lines(implicit codec: Codec): Iterator[String] = content(codec).getLines()

    def contentAsString(implicit codec: Codec): String = new String(bytes.toArray, codec)
    def `!`(implicit codec: Codec): String = contentAsString(codec)

    def appendLines(lines: String*)(implicit codec: Codec): File = Files.write(path, lines, codec, StandardOpenOption.APPEND, StandardOpenOption.CREATE)
    def append(line: String)(implicit codec: Codec): File = appendLines(line)(codec)
    def <<(line: String)(implicit codec: Codec): File = appendLines(line)(codec)
    def >>:(line: String)(implicit codec: Codec): File = appendLines(line)(codec)
    def appendNewLine: File = appendLines("")

    def write(bytes: Iterator[Byte]): File = Files.write(path, bytes.toArray) //TODO: Large I/O?

    def write(text: String)(implicit codec: Codec): File = write(text.getBytes(codec).toIterator)
    def overwrite(text: String)(implicit codec: Codec) = write(text)(codec)
    def <(text: String)(implicit codec: Codec) = write(text)(codec)
    def `>:`(text: String)(implicit codec: Codec) = write(text)(codec)

    def reader(implicit codec: Codec): BufferedReader = Files.newBufferedReader(path, codec)

    def writer(implicit codec: Codec): BufferedWriter = Files.newBufferedWriter(path, codec)

    def in: InputStream = Files.newInputStream(path)

    def out: OutputStream = Files.newOutputStream(path)

    /**
     * @return checksum of this file in hex format //TODO: make this work for directories too
     */
    def checksum(algorithm: String = "MD5"): String = DatatypeConverter.printHexBinary(MessageDigest.getInstance(algorithm).digest(bytes.toArray))

    /**
     * @return Some(target) if this is a symbolic link (to target) else None
     */
    def symLink: Option[File] = when(isSymLink)(Files.readSymbolicLink(path))

    /**
     * @return true if this file (or the file found by following symlink) is a directory
     */
    def isDirectory: Boolean = Files.isDirectory(path)

    /**
     * @return true if this file (or the file found by following symlink) is a regular file
     */
    def isRegularFile: Boolean = Files.isRegularFile(path)

    def isSymLink: Boolean = Files.isSymbolicLink(path)

    def isHidden: Boolean = Files.isHidden(path)

    // TODO:
    //def hide(): Boolean = ???
    //def unhide(): Boolean = ???

    def list: Files = file.listFiles().toIterator.map(_.toScala)
    def children: Files = list

    def listRecursively(maxDepth: Int = Int.MaxValue): Files = Files.walk(path, maxDepth)

    //TODO: Add def walk(maxDepth: Int): Stream[Path] = that ignores I/O errors and excludes self

    /**
     * Util to glob from this file's path
     * @param ignoreIOExceptions when set to true, any file visit exceptions (e.g. a read or permission error) would be silently ignored
     * @return Set of files that matched
     */
    def glob(pattern: String, syntax: String = "glob", ignoreIOExceptions: Boolean = false): Files = {
      val matcher = fileSystem.getPathMatcher(s"$syntax:$pattern")
      Files.walk(path).filter(new Predicate[Path] {override def test(path: Path) = matcher.matches(path)})
    }

    def fileSystem: FileSystem = path.getFileSystem
    def fs: FileSystem = fileSystem

    def channel: FileChannel = FileChannel.open(path)

    /**
     * @return file size (for directories, return size of the directory) in bytes
     */
    def size: Long = listRecursively().map(f => Files.size(f.path)).sum

    def permissions: Set[PosixFilePermission] = Files.getPosixFilePermissions(path).toSet

    def setPermissions(permissions: Set[PosixFilePermission]): File = Files.setPosixFilePermissions(path, permissions)

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

    def attributes      : BasicFileAttributes = Files.readAttributes(path, classOf[BasicFileAttributes])
    def posixAttributes : PosixFileAttributes = Files.readAttributes(path, classOf[PosixFileAttributes])
    def dosAttributes   : DosFileAttributes   = Files.readAttributes(path, classOf[DosFileAttributes])

    def owner: UserPrincipal = Files.getOwner(path)

    //TODO: def groups: UserPrincipal = ???

    def setOwner(owner: String): File = Files.setOwner(path, fileSystem.getUserPrincipalLookupService.lookupPrincipalByName(owner))
    val chown = setOwner _

    def setGroup(group: String): File = Files.setOwner(path, fileSystem.getUserPrincipalLookupService.lookupPrincipalByGroupName(group))
    val chgrp = setGroup _

    /**
     * Similar to the UNIX command touch - create this file if it does not exist and set its last modification time
     */
    def touch(time: Instant = Instant.now()): File = Files.setLastModifiedTime(createIfNotExists().path, FileTime.from(time))

    def lastModifiedTime: Instant = Files.getLastModifiedTime(path).toInstant

    /**
     * Deletes this file or directory
     * @param ignoreIOExceptions If this is set to true, an exception is thrown when delete fails (else it is swallowed)
     */
    def delete(ignoreIOExceptions: Boolean = false): File = {
      try {
        this match {
          case Directory(children) => children.foreach(_.delete(ignoreIOExceptions))
          case _ => file.delete()
        }
      } catch {
        case e: IOException if ignoreIOExceptions => e.printStackTrace() //swallow
      }
      this
    }

    def moveTo(destination: Path, overwrite: Boolean): File = Files.move(path, destination, copyOptions(overwrite): _*)

    def renameTo(newName: String): File = moveTo(path resolveSibling newName)

    def moveTo(destination: File, overwrite: Boolean = false): File = moveTo(destination.path, overwrite)

    def copyTo(destination: File, overwrite: Boolean = false): File = Files.copy(path, destination.path, copyOptions(overwrite): _*)

    private[this] def copyOptions(overwrite: Boolean): Seq[StandardCopyOption] = if (overwrite) Seq(StandardCopyOption.REPLACE_EXISTING) else Nil

    def symLinkTo(destination: File) = Files.createSymbolicLink(path, destination.path)

    def linkTo(destination: File, symbolic: Boolean = false): File = if (symbolic) symLinkTo(destination) else Files.createLink(path, destination.path)

    def samePathAs(that: File): Boolean = this.path == that.path

    def sameFileAs(that: File): Boolean = Files.isSameFile(this.path, that.path)

    /**
     * @return true if this file is exactly same as that file TODO: recursively for directories (or empty files?)
     */
    def contentSameAs(that: File): Boolean = this.bytes sameElements that.bytes
    val `===` = contentSameAs _

    override def equals(obj: Any) = obj match {
      case file: File => sameFileAs(file)
      case _ => false
    }

    override def hashCode = path.hashCode()

    override def toString = path.toUri.toString
  }

  object File {
    def newTempDir(prefix: String): File = Files.createTempDirectory(prefix)

    def newTemp(prefix: String, suffix: String = ""): File = Files.createTempFile(prefix, suffix)

    def apply(path: String): File = Paths.get(path)
  }

  object RegularFile {
    /**
     * @return contents of this file if it is a regular file
     */
    def unapply(file: File): Option[BufferedSource] = when(file.isRegularFile)(file.content)
  }

  object Directory {
    /**
     * @return children of this directory if file a directory
     */
    def unapply(file: File): Option[Files] = when(file.isDirectory)(file.children)
  }

  object SymbolicLink {
    /**
     * @return target of this symlink if file is a symlink
     */
    def unapply(file: File): Option[File] = file.symLink
  }

  type Files = Iterator[File]

  def root: File = FileSystems.getDefault.getRootDirectories.head
  def home: File = Properties.userHome.toFile
  def  tmp: File = Properties.tmpDir.toFile
  val `..`: File => File = _.parent
  val  `.`: File => File = identity

  implicit class StringInterpolations(sc: StringContext) {
    def file(args: Any*): File = value(args).toFile
    def resource(args: Any*): Source = Source.fromInputStream(getClass.getResourceAsStream(value(args)))
    private[this] def value(args: Seq[Any]) = sc.s(args: _*)
  }

  implicit class StringOps(str: String) {
    def toFile: File = File(str)
    def /(child: String): File = toFile/child
  }

  object Cmds {
    def cp(file1: File, file2: File): File = file1.copyTo(file2, overwrite = true)
    def mv(file1: File, file2: File): File = file1.moveTo(file2, overwrite = true)
    def rm(file: File): File = file.delete(ignoreIOExceptions = true)
    def ln(file1: File, file2: File): File = file1 linkTo file2
    def ln_s(file1: File, file2: File): File = file1 symLinkTo file2
    def cat(files: File*): Seq[Iterator[Byte]] = files.map(_.bytes)
    def ls(file: File): Files = file.list
    def ls_r(file: File): Files = file.listRecursively()
    def touch(file: File): File = file.touch()
    def mkdir(file: File): File = file.mkdirs()
    def chown(owner: String, file: File): File = file.setOwner(owner)
    def chgrp(group: String, file: File): File = file.setGroup(group)
    def chmod_+(permission: PosixFilePermission, file: File): File = file.addPermissions(permission)
    def chmod_-(permission: PosixFilePermission, file: File): File = file.removePermissions(permission)
  }

  implicit class FileOps(file: JFile) {
    def toScala: File = File(file)
  }

  implicit class InputStreamOps(in: InputStream) {
    def >(out: OutputStream): Unit = pipeTo(out)

    def pipeTo(out: OutputStream, bufferSize: Int = 1<<10): Unit = pipeTo(out, Array.ofDim[Byte](bufferSize))

    /**
     * Pipe an input stream to an output stream using a byte buffer
     */
    @tailrec final def pipeTo(out: OutputStream, buffer: Array[Byte]): Unit = in.read(buffer) match {
      case n if n > 0 =>
        out.write(buffer, 0, n)
        pipeTo(out, buffer)
      case _ =>
        in.close()
        out.close()
    }

    def buffered: BufferedInputStream = new BufferedInputStream(in)

    def reader(implicit codec: Codec): InputStreamReader = new InputStreamReader(in, codec)
  }

  implicit class OutputStreamOps(out: OutputStream) {
    def buffered: BufferedOutputStream = new BufferedOutputStream(out)

    def writer(implicit codec: Codec): OutputStreamWriter = new OutputStreamWriter(out, codec)
  }

  implicit class ReaderOps(reader: Reader) {
    def buffered: BufferedReader = new BufferedReader(reader)
  }

  implicit class WriterOps(writer: Writer) {
    def buffered: BufferedWriter = new BufferedWriter(writer)
  }

  type Closeable = {
    def close(): Unit //TODO: ARM
  }

  implicit def codecToCharSet(codec: Codec): Charset = codec.charSet

  implicit def pathToFile(path: Path): File = path.toFile.toScala

  private[files] implicit def pathStreamToFiles(files: JStream[Path]): Files = files.iterator().map(pathToFile)
  private[files] def when[A](condition: Boolean)(f: => A): Option[A] = if (condition) Some(f) else None
}
