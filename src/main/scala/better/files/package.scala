package better

import java.io.{File => JFile, FileSystem => JFileSystem, _}
import java.net.URI
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file._, attribute._
import java.nio.charset.Charset
import java.security.MessageDigest
import java.time.Instant
import java.util.function.Predicate
import java.util.stream.{Stream => JStream}
import java.util.zip._
import javax.xml.bind.DatatypeConverter

import akka.actor.{Props, ActorRef, ActorSystem}

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.io.{BufferedSource, Codec, Source}
import scala.util.Properties

package object files {
  import Closeable.managed
  /**
   * Scala wrapper around java.nio.files.Path
   */
  implicit class File(val path: Path) {
    def fullPath: String = path.toString

    //def normalizedPath: File = path.normalize()
    //def absolutePath: File = path.toAbsolutePath //TODO: use this?

    def toJava: JFile = path.toFile

    def name: String = path.getFileName.toString

    def root: File = path.getRoot

    def nameWithoutExtension: String = if (hasExtension) name.substring(0, name lastIndexOf ".") else name

    /**
     * @return extension (including the dot) of this file if it is a regular file and has an extension, else None
     */
    def extension: Option[String] = when(hasExtension)(name.substring(name indexOf ".").toLowerCase)

    def hasExtension: Boolean = isRegularFile && (name contains ".")

    /**
     * Changes the file-extension by renaming this file; if file does not have an extension, it adds the extension
     * Example usage file"foo.java".changeExtensionTo(".scala")
     */
    def changeExtensionTo(extension: String): File = if (isRegularFile) renameTo(s"$nameWithoutExtension$extension") else this

    def contentType: Option[String] = Option(Files.probeContentType(path))

    def parent: File = path.getParent

    def /(child: String): File = path.resolve(child)

    def /(f: File => File): File = f(this)

    def createChild(child: String, asDirectory: Boolean = false): File = (this / child).createIfNotExists(asDirectory)

    def createIfNotExists(asDirectory: Boolean = false): File = if (exists) {
      this
    } else if (asDirectory) {
      createDirectories()
    } else {
      parent.createDirectories()
      Files.createFile(path)
    }

    def exists: Boolean = Files.exists(path)

    def notExists: Boolean = Files.notExists(path)

    def isChildOf(parent: File): Boolean = parent isParentOf this

    /**
     * Check if this directory contains this file
     * @param file
     * @return true if this is a directory and it contains this file
     */
    def contains(file: File): Boolean = isDirectory && (file.path startsWith path)
    def isParentOf(child: File): Boolean = contains(child)

    def content(implicit codec: Codec): BufferedSource = Source.fromFile(toJava)(codec)
    def source(implicit codec: Codec): BufferedSource = content(codec)

    def bytes: Iterator[Byte] = in.buffered.bytes

    def loadBytes: Array[Byte] = Files.readAllBytes(path)
    def byteArray: Array[Byte] = loadBytes

    def createDirectory(): File = Files.createDirectory(path)

    def createDirectories(): File = Files.createDirectories(path)

    def chars(implicit codec: Codec): Iterator[Char] = content(codec)

    def lines(implicit codec: Codec): Iterator[String] = content(codec).getLines()

    def contentAsString(implicit codec: Codec): String = new String(byteArray, codec)
    def `!`(implicit codec: Codec): String = contentAsString(codec)

    def appendLines(lines: String*)(implicit codec: Codec): File = Files.write(path, lines, codec, StandardOpenOption.APPEND, StandardOpenOption.CREATE)
    def <<(line: String)(implicit codec: Codec): File = appendLines(line)(codec)
    def >>:(line: String)(implicit codec: Codec): File = appendLines(line)(codec)
    def appendNewLine()(implicit codec: Codec): File = appendLine("")
    def appendLine(line: String)(implicit codec: Codec): File = appendLines(line)

    def append(text: String)(implicit codec: Codec): File = append(text.getBytes(codec))

    def append(bytes: Array[Byte]): File = Files.write(path, bytes, StandardOpenOption.APPEND, StandardOpenOption.CREATE)

    /**
     * Write byte array to file. For large files, piping in streams is recommended
     * @param bytes
     * @return this
     */
    def write(bytes: Array[Byte]): File = Files.write(path, bytes)

    def write(text: String)(implicit codec: Codec): File = write(text.getBytes(codec))
    def overwrite(text: String)(implicit codec: Codec) = write(text)(codec)
    def <(text: String)(implicit codec: Codec) = write(text)(codec)
    def `>:`(text: String)(implicit codec: Codec) = write(text)(codec)

    def newRandomAccess(mode: String = "r"): RandomAccessFile = new RandomAccessFile(toJava, mode)
    def randomAccess(mode: String = "r"): RandomAccessFile = newRandomAccess(mode)

    def newBufferedReader(implicit codec: Codec): BufferedReader = Files.newBufferedReader(path, codec)
    def reader(implicit codec: Codec): BufferedReader = newBufferedReader(codec)

    def newBufferedWriter(implicit codec: Codec): BufferedWriter = Files.newBufferedWriter(path, codec)

    def newFileWriter(append: Boolean = false): FileWriter = new FileWriter(toJava, append)

    def newInputStream: InputStream = Files.newInputStream(path)
    def in: InputStream = newInputStream

    def newScanner(delimiter: String = Scanner.defaultDelimiter, includeDelimiters: Boolean = false)(implicit codec: Codec): Scanner = new Scanner(this, delimiter, includeDelimiters)(codec)
    def scanner(delimiter: String = Scanner.defaultDelimiter, includeDelimiters: Boolean = false)(implicit codec: Codec): Scanner = newScanner(delimiter, includeDelimiters)(codec)

    def newOutputStream: OutputStream = Files.newOutputStream(path)
    def out: OutputStream = newOutputStream

    def newWatchService: WatchService = fileSystem.newWatchService()

    def newWatchKey(events: WatchEvent.Kind[_]*): WatchKey = path.register(newWatchService, events.toArray)

    def watcherProps(recursive: Boolean): Props = Props(new FileWatcher(this, recursive))

    def newWatcher(recursive: Boolean = true)(implicit system: ActorSystem): ActorRef = system.actorOf(watcherProps(recursive))

    def digest(algorithm: String): Array[Byte] = {
      val digestor = MessageDigest.getInstance(algorithm)
      listRelativePaths.toSeq.sorted foreach {relativePath =>
        val file = path resolve relativePath
        val bytes = if (Files.isDirectory(file)) relativePath.toString.getBytes else Files.readAllBytes(file)
        digestor.update(bytes)
      }
      digestor.digest()
    }

    /**
     * @return checksum of this file (or directory) in hex format
     */
    def checksum(algorithm: String): String = DatatypeConverter.printHexBinary(digest(algorithm))

    def md5: String = checksum("MD5")

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

    def list: Files = Files.newDirectoryStream(path).iterator() map pathToFile
    def children: Files = list
    def entries: Files = list

    def listRecursively: Files = walk().filterNot(samePathAs)

    /**
     * Walk the directory tree recursively upto maxDepth
     * @param maxDepth
     * @return List of children in BFS maxDepth level deep (includes self since self is at depth = 0)
     */
    def walk(maxDepth: Int = Int.MaxValue): Files = Files.walk(path, maxDepth) //TODO: that ignores I/O errors?

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

    def newFileChannel: FileChannel = FileChannel.open(path)
    def channel: FileChannel = newFileChannel

    def uri: URI = path.toUri

    /**
     * @return file size (for directories, return size of the directory) in bytes
     */
    def size: Long = walk().map(f => Files.size(f.path)).sum

    def permissions: Set[PosixFilePermission] = Files.getPosixFilePermissions(path).toSet

    def setPermissions(permissions: Set[PosixFilePermission]): File = Files.setPosixFilePermissions(path, permissions)

    def addPermission(permission: PosixFilePermission): File = setPermissions(permissions + permission)

    def removePermission(permission: PosixFilePermission): File = setPermissions(permissions - permission)

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

    def ownerName: String = owner.getName

    def group: GroupPrincipal = posixAttributes.group()

    def groupName: String = group.getName

    def setOwner(owner: String): File = Files.setOwner(path, fileSystem.getUserPrincipalLookupService.lookupPrincipalByName(owner))

    def setGroup(group: String): File = Files.setOwner(path, fileSystem.getUserPrincipalLookupService.lookupPrincipalByGroupName(group))

    /**
     * Similar to the UNIX command touch - create this file if it does not exist and set its last modification time
     */
    def touch(time: Instant = Instant.now()): File = Files.setLastModifiedTime(createIfNotExists().path, FileTime.from(time))

    def lastModifiedTime: Instant = Files.getLastModifiedTime(path).toInstant

    /**
     * Deletes this file or directory
     * @param ignoreIOExceptions If this is set to true, an exception is thrown when delete fails (else it is swallowed)
     */
    def delete(ignoreIOExceptions: Boolean = false): File = returning(this) {
      try {
        if (isDirectory) list.foreach(_.delete(ignoreIOExceptions))
        Files.delete(path)
      } catch {
        case e: IOException if ignoreIOExceptions => e.printStackTrace() //swallow
      }
    }

    def renameTo(newName: String): File = moveTo(path resolveSibling newName)

    /**
     *
     * @param destination
     * @param overwrite
     * @return destination
     */
    def moveTo(destination: File, overwrite: Boolean = false): File = Files.move(path, destination.path, copyOptions(overwrite): _*)

    /**
     *
     * @param destination
     * @param overwrite
     * @return destination
     */
    def copyTo(destination: File, overwrite: Boolean = false): File = if(isDirectory) {
      if (overwrite) destination.delete(ignoreIOExceptions = true)
      Files.walkFileTree(path, new SimpleFileVisitor[Path] {
        def newPath(subPath: Path): Path = destination.path resolve (path relativize subPath)

        override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = {
          Files.createDirectories(newPath(dir))
          super.preVisitDirectory(dir, attrs)
        }

        override def visitFile(file: Path, attrs: BasicFileAttributes) = {
          Files.copy(file, newPath(file), copyOptions(overwrite): _*)
          super.visitFile(file, attrs)
        }
      })
      destination
    } else {
      Files.copy(path, destination.path, copyOptions(overwrite): _*)
    }

    private[this] def copyOptions(overwrite: Boolean): Seq[StandardCopyOption] = if (overwrite) Seq(StandardCopyOption.REPLACE_EXISTING) else Nil

    def symbolicLinkTo(destination: File) = Files.createSymbolicLink(path, destination.path)

    def linkTo(destination: File, symbolic: Boolean = false): File = if (symbolic) symbolicLinkTo(destination) else Files.createLink(path, destination.path)

    def samePathAs(that: File): Boolean = this.path == that.path

    def sameFileAs(that: File): Boolean = Files.isSameFile(this.path, that.path)

    def listRelativePaths: Iterator[Path] = walk().map(relativize)

    def relativize(destination: File): Path = path relativize destination.path

    /**
     * @return true if this file is exactly same as that file
     *         For directories, it checks for equivalent directory structure
     *         Note: Since it uses md5 underneath, there is a small chance of an md5 collision for different files (TODO)
     */
    def sameContentAs(that: File): Boolean = this.md5 == that.md5
    def `===`(that: File): Boolean = sameContentAs(that)

    def =!=(that: File): Boolean = !sameContentAs(that)

    override def equals(obj: Any) = obj match {
      case file: File => samePathAs(file)
      case _ => false
    }

    /**
     * @return true if file is not present or empty directory or 0-bytes file
     */
    def isEmpty: Boolean = this match {
      case Directory(children) => children.isEmpty
      case RegularFile(content) => content.isEmpty
      case _ => notExists
    }

    /**
     * If this is a directory, remove all its children
     * If its a file, empty the contents
     * @return this
     */
    def clear(): File = returning(this) {
      this match {
        case Directory(children) => children.foreach(_.delete())
        case _ => write(Array.ofDim[Byte](0))
      }
    }

    override def hashCode = path.hashCode()

    override def toString = uri.toString

    /**
     * Zips this file (or directory)
     *
     * @param destination The destination file; Creates this if it does not exists
     * @return The destination zip file
     */
    def zipTo(destination: File): File = {
      val files = if (isDirectory) children.toSeq else Seq(this)
      Cmds.zip(files: _*)(destination)
    }

    /**
     * zip to a temp directory
     * @return the target directory
     */
    def zip(): File = zipTo(destination = File.newTemp(name, ".zip"))

    /**
     * Unzips this zip file
     *
     * @param destination destination folder; Creates this if it does not exist
     * @return The destination where contents are unzipped
     */
    def unzipTo(destination: File): File = returning(destination) {
      for {
        zipFile <- managed(new ZipFile(toJava))
        entry <- zipFile.entries()
        file = destination.createChild(entry.getName, entry.isDirectory)
        if !entry.isDirectory
      } zipFile.getInputStream(entry) > file.out
    }

    /**
     * unzip to a temporary zip file
     * @return the zip file
     */
    def unzip(): File = unzipTo(destination = File.newTempDir(name))
  }

  object File {
    def newTempDir(prefix: String = ""): File = Files.createTempDirectory(prefix)

    def newTemp(prefix: String = "", suffix: String = ""): File = Files.createTempFile(prefix, suffix)

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
  def   ~ : File = home
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
    def del(file: File): File = rm(file)

    def ln(file1: File, file2: File): File = file1 linkTo file2

    def ln_s(file1: File, file2: File): File = file1 symbolicLinkTo file2

    def cat(files: File*): Seq[Iterator[Byte]] = files.map(_.bytes)

    def ls(file: File): Files = file.list
    def dir(file: File): Files = ls(file)

    def ls_r(file: File): Files = file.listRecursively

    def touch(file: File): File = file.touch()

    def mkdir(file: File): File = file.createDirectory()

    def md5(file: File): String = file.md5

    def mkdirs(file: File): File = file.createDirectories()

    def chown(owner: String, file: File): File = file.setOwner(owner)

    def chgrp(group: String, file: File): File = file.setGroup(group)

    def chmod_+(permission: PosixFilePermission, file: File): File = file.addPermission(permission)

    def chmod_-(permission: PosixFilePermission, file: File): File = file.removePermission(permission)

    def unzip(zipFile: File)(destination: File): File = zipFile unzipTo destination

    def zip(files: File*)(destination: File): File = returning(destination) {
      for {
        out <- managed(new ZipOutputStream(destination.out))
        input <- files
        file <- input.walk()
        name = input.parent relativize file
      } out.add(file, name = Some(name.toString))
    }
  }

  implicit class FileOps(file: JFile) {
    def toScala: File = File(file.getPath)
  }

  implicit class InputStreamOps(in: InputStream) {
    def >(out: OutputStream): Unit = pipeTo(out)

    def pipeTo(out: OutputStream, closeOutputStream: Boolean = true, bufferSize: Int = 1<<10): Unit = pipeTo(out, closeOutputStream, Array.ofDim[Byte](bufferSize))

    /**
     * Pipe an input stream to an output stream using a byte buffer
     */
    @tailrec final def pipeTo(out: OutputStream, closeOutputStream: Boolean, buffer: Array[Byte]): Unit = in.read(buffer) match {
      case n if n > 0 =>
        out.write(buffer, 0, n)
        pipeTo(out, closeOutputStream, buffer)
      case _ =>
        in.close()
        if(closeOutputStream) out.close()
    }

    def buffered: BufferedInputStream = new BufferedInputStream(in)

    def gzipped: GZIPInputStream = new GZIPInputStream(in)

    def reader(implicit codec: Codec): InputStreamReader = new InputStreamReader(in, codec)

    def content(implicit codec: Codec): BufferedSource = Source.fromInputStream(in)(codec)

    def lines(implicit codec: Codec): Iterator[String] = content(codec).getLines()

    def bytes: Iterator[Byte] = {
      var isClosed = false
      def hasMore(byte: Int) = {
        if(!isClosed && byte == -1) {
          in.close()
          isClosed = true
        }
        !isClosed
      }
      Iterator.continually(in.read()).takeWhile(hasMore).map(_.toByte)
    }
  }

  implicit class OutputStreamOps(out: OutputStream) {
    def buffered: BufferedOutputStream = new BufferedOutputStream(out)

    def gzipped: GZIPOutputStream = new GZIPOutputStream(out)

    def writer(implicit codec: Codec): OutputStreamWriter = new OutputStreamWriter(out, codec)

    def printer(autoFlush: Boolean = false): PrintWriter = new PrintWriter(out, autoFlush)
  }

  implicit class ReaderOps(reader: Reader) {
    def buffered: BufferedReader = new BufferedReader(reader)
  }

  implicit class WriterOps(writer: Writer) {
    def buffered: BufferedWriter = new BufferedWriter(writer)
  }

  implicit class FileChannelOps(fc: FileChannel) {
    def toMappedByteBuffer: MappedByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())
  }

  implicit class ZipOutputStreamOps(out: ZipOutputStream) {

    def add(file: File, name: Option[String] = None): ZipOutputStream = returning(out) {
      val relativeName = (name getOrElse file.name).stripSuffix(file.fileSystem.getSeparator)
      val entryName = if (file.isDirectory) s"$relativeName/" else relativeName // make sure to end directories in ZipEntry with "/"
      out.putNextEntry(new ZipEntry(entryName))
      if (file.isRegularFile) file.newInputStream.pipeTo(out, closeOutputStream = false)
      out.closeEntry()
    }
  }

  type Closeable = {
    def close(): Unit
  }

  object Closeable {
    /**
     * Lightweight automatic resource management
     * Closes the resource when done
     * e.g.
     * <pre>
     * ``
     * for {
     *   in <- managed(file.newInputStream)
     * } in.write(bytes)
     * // in is closed now
     * ``
     * </pre>
     * @param resource
     * @return
     */
    def managed[A <: Closeable](resource: A): Traversable[A] = new Traversable[A] {
      override def foreach[U](f: A => U) = try {
        f(resource)
      } finally {
        import scala.language.reflectiveCalls
        resource.close()
      }
    }
  }

  implicit def codecToCharSet(codec: Codec): Charset = codec.charSet

  private[files] def pathToFile(path: Path): File = path
  private[files] implicit def pathStreamToFiles(files: JStream[Path]): Files = files.iterator().map(pathToFile)

  def when(events: FileWatcher.Event*)(callback: FileWatcher.Callback) = FileWatcher.RegisterCallback(events.distinct, callback)

  def on(event: FileWatcher.Event)(callback: (File => Unit)) = when(event){case (`event`, file) => callback(file)}

  // Some utils:
  @inline private[files] def when[A](condition: Boolean)(f: => A): Option[A] = if (condition) Some(f) else None
  @inline private[files] def repeat[A](n: Int)(f: => A): Seq[A] = (1 to n).map(_ => f)
  @inline private[files] def returning[A](obj: A)(f: => Unit): A = {f; obj}
}
