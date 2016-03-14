package better.files

import java.io.{File => JFile, FileSystem => JFileSystem, _} //TODO: Scala 2.10 does not like java.io._
import java.net.URI
import java.nio.channels.{OverlappingFileLockException, AsynchronousFileChannel, FileChannel, NonWritableChannelException, NonReadableChannelException}
import java.nio.file._, attribute._
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.{Deflater, ZipFile}
import javax.xml.bind.DatatypeConverter

import scala.collection.JavaConversions._
import scala.io.{BufferedSource, Codec, Source}
import scala.util.Properties

/**
 * Scala wrapper around java.nio.files.Path
 */
class File private (val path: Path) { //TODO: LinkOption?

  def pathAsString: String = path.toString

  def toJava: JFile = path.toFile

  def name: String = path.getFileName.toString

  def root: File = path.getRoot

  def nameWithoutExtension: String = if (hasExtension) name.substring(0, name lastIndexOf ".") else name

  /**
   * @return extension (including the dot) of this file if it is a regular file and has an extension, else None
   */
  def extension: Option[String] = extension()

  /**
   * @param includeDot whether the dot should be included in the extension or not
   * @param includeAll whether all extension tokens should be included, or just the last one
   * @param toLowerCase to lowercase the extension or not e.g. foo.HTML should have .html or .HTML
   * @return extension of this file if it is a regular file and has an extension, else None
   */
  def extension(includeDot: Boolean = true, includeAll: Boolean = false, toLowerCase: Boolean = true): Option[String] = when(hasExtension) {
    val dot = if (includeAll) name indexOf "." else name lastIndexOf "."
    val index = if (includeDot) dot else dot + 1
    val extension = name.substring(index)
    if (toLowerCase) extension.toLowerCase else extension
  }

  def hasExtension: Boolean = (isRegularFile || notExists) && (name contains ".")

  /**
   * Changes the file-extension by renaming this file; if file does not have an extension, it adds the extension
   * Example usage file"foo.java".changeExtensionTo(".scala")
   */
  def changeExtensionTo(extension: String): File = if (isRegularFile) renameTo(s"$nameWithoutExtension$extension") else this

  def contentType: Option[String] = Option(Files.probeContentType(path))

  /**
   * Return parent of this file
   * NOTE: This API returns null if this file is the root;
   *       please wrap it in an Option if you expect to handle such behaviour
   * @see parentOption
   * @return
   */
  def parent: File = parentOption.orNull

  /**
   *
   * @return Some(parent) of this file or None if this is the root and thus has no parent
   */
  def parentOption: Option[File] = Option(path.getParent).map(File.apply)

  def /(child: String): File = path.resolve(child)

  def createChild(child: String, asDirectory: Boolean = false)(implicit attributes: File.Attributes = File.Attributes.default): File = (this / child).createIfNotExists(asDirectory)(attributes)

  def createIfNotExists(asDirectory: Boolean = false)(implicit attributes: File.Attributes = File.Attributes.default): File = if (exists) {
    this
  } else if (asDirectory) {
    createDirectories()(attributes)
  } else {
    parent.createDirectories()
    Files.createFile(path, attributes: _*)
  }

  def exists(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean = Files.exists(path, linkOptions: _*)

  def notExists(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean = Files.notExists(path, linkOptions: _*)

  def sibling(name: String): File = path resolveSibling name

  def isSiblingOf(sibling: File): Boolean = sibling isChildOf parent

  def siblings: Files = parent.list.filterNot(_ == this)

  def isChildOf(parent: File): Boolean = parent isParentOf this

  /**
   * Check if this directory contains this file
   * @param file
   * @return true if this is a directory and it contains this file
   */
  def contains(file: File): Boolean = isDirectory && (file.path startsWith path)
  def isParentOf(child: File): Boolean = contains(child)

  def bytes: Iterator[Byte] = newInputStream.buffered.bytes //TODO: ManagedResource here?

  def loadBytes: Array[Byte] = Files.readAllBytes(path)
  def byteArray: Array[Byte] = loadBytes

  def createDirectory()(implicit attributes: File.Attributes = File.Attributes.default): File = Files.createDirectory(path, attributes: _*)

  def createDirectories()(implicit attributes: File.Attributes = File.Attributes.default): File = Files.createDirectories(path, attributes: _*)

  def chars(implicit codec: Codec): Iterator[Char] = newBufferedReader(codec).chars //TODO: ManagedResource here?

  /**
   * Load all lines from this file
   * Note: Large files may cause an OutOfMemory in which case, use the streaming version @see lineIterator
   *
   * @param codec
   * @return all lines in this file
   */
  def lines(implicit codec: Codec): Traversable[String] = Files.readAllLines(path, codec)

  /**
   * Iterate over lines in a file (auto-close stream on complete)
   * NOTE: If the iteration is partial, it may leave a stream open
   *       If you want partial iteration use @see lines()
   * @param codec
   * @return
   */
  def lineIterator(implicit codec: Codec): Iterator[String] = Files.lines(path, codec).toAutoClosedIterator

  def tokens(implicit config: Scanner.Config = Scanner.Config.default, codec: Codec): Traversable[String] = bufferedReader(codec).flatMap(_.tokens(config))

  def contentAsString(implicit codec: Codec): String = new String(byteArray, codec)
  def `!`(implicit codec: Codec): String = contentAsString(codec)

  def appendLines(lines: String*)(implicit openOptions: File.OpenOptions = File.OpenOptions.append, codec: Codec): File = Files.write(path, lines, codec, openOptions: _*)

  def <<(line: String)(implicit openOptions: File.OpenOptions = File.OpenOptions.append, codec: Codec): File = appendLines(line)(openOptions, codec)

  def >>:(line: String)(implicit openOptions: File.OpenOptions = File.OpenOptions.append, codec: Codec): File = appendLines(line)(openOptions, codec)

  def appendNewLine()(implicit openOptions: File.OpenOptions = File.OpenOptions.append, codec: Codec): File = appendLine("")(openOptions, codec)

  def appendLine(line: String)(implicit openOptions: File.OpenOptions = File.OpenOptions.append, codec: Codec): File = appendLines(line)(openOptions, codec)

  def append(text: String)(implicit openOptions: File.OpenOptions = File.OpenOptions.append, codec: Codec): File = append(text.getBytes(codec))(openOptions)

  def append(bytes: Array[Byte])(implicit openOptions: File.OpenOptions): File = Files.write(path, bytes, openOptions: _*)

  /**
   * Write byte array to file. For large files, piping in streams is recommended
   * @param bytes
   * @return this
   */
  def write(bytes: Array[Byte])(implicit openOptions: File.OpenOptions): File = Files.write(path, bytes, openOptions: _*)

  def write(text: String)(implicit openOptions: File.OpenOptions = File.OpenOptions.default, codec: Codec): File = write(text.getBytes(codec))(openOptions)

  def overwrite(text: String)(implicit openOptions: File.OpenOptions = File.OpenOptions.default, codec: Codec) = write(text)(openOptions, codec)

  def <(text: String)(implicit openOptions: File.OpenOptions = File.OpenOptions.default, codec: Codec) = write(text)(openOptions, codec)

  def `>:`(text: String)(implicit openOptions: File.OpenOptions = File.OpenOptions.default, codec: Codec) = write(text)(openOptions, codec)

  //TODO: @managed macro

  def newBufferedSource(implicit codec: Codec): BufferedSource = Source.fromFile(toJava)(codec)

  def bufferedSource(implicit codec: Codec): ManagedResource[BufferedSource] = newBufferedSource(codec).autoClosed

  def newRandomAccess(mode: File.RandomAccessMode = File.RandomAccessMode.read): RandomAccessFile = new RandomAccessFile(toJava, mode.value)

  def randomAccess(mode: File.RandomAccessMode = File.RandomAccessMode.read): ManagedResource[RandomAccessFile] = newRandomAccess(mode).autoClosed //TODO: Mode enum?

  def newBufferedReader(implicit codec: Codec): BufferedReader = Files.newBufferedReader(path, codec)

  def bufferedReader(implicit codec: Codec): ManagedResource[BufferedReader] = newBufferedReader(codec).autoClosed

  def newBufferedWriter(implicit codec: Codec, openOptions: File.OpenOptions = File.OpenOptions.default): BufferedWriter = Files.newBufferedWriter(path, codec, openOptions: _*)

  def bufferedWriter(implicit codec: Codec, openOptions: File.OpenOptions = File.OpenOptions.default): ManagedResource[BufferedWriter] = newBufferedWriter(codec, openOptions).autoClosed

  def newFileReader: FileReader = new FileReader(toJava)

  def fileReader: ManagedResource[FileReader] = newFileReader.autoClosed

  def newFileWriter(append: Boolean = false): FileWriter = new FileWriter(toJava, append)

  def fileWriter(append: Boolean = false): ManagedResource[FileWriter] = newFileWriter(append).autoClosed

  def newInputStream(implicit openOptions: File.OpenOptions = File.OpenOptions.default): InputStream = Files.newInputStream(path, openOptions: _*)

  def inputStream(implicit openOptions: File.OpenOptions = File.OpenOptions.default): ManagedResource[InputStream] = newInputStream(openOptions).autoClosed

  def newScanner(implicit config: Scanner.Config = Scanner.Config.default): Scanner = Scanner(newBufferedReader(config.codec))(config)

  def scanner(implicit config: Scanner.Config = Scanner.Config.default): ManagedResource[Scanner] = newScanner(config).autoClosed

  def newOutputStream(implicit openOptions: File.OpenOptions = File.OpenOptions.default): OutputStream = Files.newOutputStream(path, openOptions: _*)

  def outputStream(implicit openOptions: File.OpenOptions = File.OpenOptions.default): ManagedResource[OutputStream] = newOutputStream(openOptions).autoClosed

  def newFileChannel(implicit openOptions: File.OpenOptions = File.OpenOptions.default, attributes: File.Attributes = File.Attributes.default): FileChannel = FileChannel.open(path, openOptions.toSet, attributes: _*)

  def fileChannel(implicit openOptions: File.OpenOptions = File.OpenOptions.default, attributes: File.Attributes = File.Attributes.default): ManagedResource[FileChannel] = newFileChannel(openOptions, attributes).autoClosed

  def newAsynchronousFileChannel(implicit openOptions: File.OpenOptions = File.OpenOptions.default): AsynchronousFileChannel = AsynchronousFileChannel.open(path, openOptions: _*)

  def asynchronousFileChannel(implicit openOptions: File.OpenOptions = File.OpenOptions.default): ManagedResource[AsynchronousFileChannel] = newAsynchronousFileChannel(openOptions).autoClosed

  def newWatchService: WatchService = fileSystem.newWatchService()

  def watchService: ManagedResource[WatchService] = newWatchService.autoClosed

  def register(service: WatchService, events: File.Events = File.Events.all): File = returning(this) {
    path.register(service, events.toArray)
  }

  def digest(algorithmName: String): Array[Byte] = {
    val algorithm = MessageDigest.getInstance(algorithmName)
    listRelativePaths.toSeq.sorted foreach {relativePath =>
      val file: File = path resolve relativePath
      val bytes = if (file.isDirectory) relativePath.toString.getBytes else file.loadBytes
      algorithm.update(bytes)
    }
    algorithm.digest()
  }

  /**
   * @return checksum of this file (or directory) in hex format
   */
  def checksum(algorithm: String): String = DatatypeConverter.printHexBinary(digest(algorithm))

  def md5 = checksum("MD5")

  def sha1 = checksum("SHA-1")

  def sha256 = checksum("SHA-256")

  def sha512 = checksum("SHA-512")

  /**
   * @return Some(target) if this is a symbolic link (to target) else None
   */
  def symbolicLink: Option[File] = when(isSymbolicLink)(Files.readSymbolicLink(path))

  /**
   * @return true if this file (or the file found by following symlink) is a directory
   */
  def isDirectory(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean = Files.isDirectory(path, linkOptions: _*)

  /**
   * @return true if this file (or the file found by following symlink) is a regular file
   */
  def isRegularFile(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean = Files.isRegularFile(path, linkOptions: _*)

  def isSymbolicLink: Boolean = Files.isSymbolicLink(path)

  def isHidden: Boolean = Files.isHidden(path)

  def isLocked(mode: File.RandomAccessMode, position: Long = 0L, size: Long = Long.MaxValue, isShared: Boolean = false): Boolean = {
    val channel = newRandomAccess(mode).getChannel
    try {
      channel.tryLock(position, size, isShared).release()
      false
    } catch {
      case _: OverlappingFileLockException | _: NonWritableChannelException | _: NonReadableChannelException => true
    } finally {
      channel.close()
    }
  }

  def isReadLocked(position: Long = 0L, size: Long = Long.MaxValue, isShared: Boolean = false) = isLocked(File.RandomAccessMode.read, position, size, isShared)

  def isWriteLocked(position: Long = 0L, size: Long = Long.MaxValue, isShared: Boolean = false) = isLocked(File.RandomAccessMode.readWrite, position, size, isShared)

  def list: Files = Files.list(path)
  def children: Files = list
  def entries: Files = list

  def listRecursively(implicit visitOptions: File.VisitOptions = File.VisitOptions.default): Files = walk()(visitOptions).filterNot(isSamePathAs)

  /**
   * Walk the directory tree recursively upto maxDepth
   * @param maxDepth
   * @return List of children in BFS maxDepth level deep (includes self since self is at depth = 0)
   */
  def walk(maxDepth: Int = Int.MaxValue)(implicit visitOptions: File.VisitOptions = File.VisitOptions.default): Files = Files.walk(path, maxDepth, visitOptions: _*) //TODO: that ignores I/O errors?

  def pathMatcher(syntax: File.PathMatcherSyntax)(pattern: String): PathMatcher = fileSystem.getPathMatcher(s"${syntax.name}:$pattern")

  /**
   * Util to glob from this file's path
   * @return Set of files that matched
   */
  def glob(pattern: String)(implicit syntax: File.PathMatcherSyntax = File.PathMatcherSyntax.default): Files = {
    val matcher = pathMatcher(syntax)(pattern)
    collectChildren(child => matcher.matches(child.path))
  }

  /**
   * More Scala friendly way of doing Files.walk
   * @param f
   * @return
   */
  def collectChildren(f: File => Boolean): Files = Files.walk(path).filter(new java.util.function.Predicate[Path] {override def test(path: Path) = f(path)}) //TODO: In Scala 2.11 SAM: Files.walk(path).filter(f(_))

  def fileSystem: FileSystem = path.getFileSystem

  def uri: URI = path.toUri

  /**
   * @return file size (for directories, return size of the directory) in bytes
   */
  def size(implicit visitOptions: File.VisitOptions = File.VisitOptions.default): Long = walk()(visitOptions).map(f => Files.size(f.path)).sum

  def permissions(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Set[PosixFilePermission] = Files.getPosixFilePermissions(path, linkOptions: _*).toSet

  def permissionsAsString(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): String = PosixFilePermissions.toString(permissions(linkOptions))

  def setPermissions(permissions: Set[PosixFilePermission]): File = Files.setPosixFilePermissions(path, permissions)

  def addPermission(permission: PosixFilePermission)(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): File = setPermissions(permissions(linkOptions) + permission)

  def removePermission(permission: PosixFilePermission)(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): File = setPermissions(permissions(linkOptions) - permission)

  /**
   * test if file has this permission
   */
  def apply(permission: PosixFilePermission)(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean = permissions(linkOptions)(permission)

  def isOwnerReadable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean = this(PosixFilePermission.OWNER_READ)(linkOptions)

  def isOwnerWritable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean = this(PosixFilePermission.OWNER_WRITE)(linkOptions)

  def isOwnerExecutable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean = this(PosixFilePermission.OWNER_EXECUTE)(linkOptions)

  def isGroupReadable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean = this(PosixFilePermission.GROUP_READ)(linkOptions)

  def isGroupWritable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean = this(PosixFilePermission.GROUP_WRITE)(linkOptions)

  def isGroupExecutable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean = this(PosixFilePermission.GROUP_EXECUTE)(linkOptions)

  def isOtherReadable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean = this(PosixFilePermission.OTHERS_READ)(linkOptions)

  def isOtherWritable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean = this(PosixFilePermission.OTHERS_WRITE)(linkOptions)

  def isOtherExecutable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean = this(PosixFilePermission.OTHERS_EXECUTE)(linkOptions)

  /**
   * This differs from the above as this checks if the JVM can read this file even though the OS cannot in certain platforms
   * @see isOwnerReadable
   * @return
   */
  def isReadable: Boolean = toJava.canRead
  def isWriteable: Boolean = toJava.canWrite
  def isExecutable: Boolean = toJava.canExecute

  def attributes(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): BasicFileAttributes = Files.readAttributes(path, classOf[BasicFileAttributes], linkOptions: _*)

  def posixAttributes(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): PosixFileAttributes = Files.readAttributes(path, classOf[PosixFileAttributes], linkOptions: _*)

  def dosAttributes(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): DosFileAttributes   = Files.readAttributes(path, classOf[DosFileAttributes], linkOptions: _*)

  def owner(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): UserPrincipal = Files.getOwner(path, linkOptions: _*)

  def ownerName(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): String = owner(linkOptions).getName

  def group(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): GroupPrincipal = posixAttributes(linkOptions).group()

  def groupName(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): String = group(linkOptions).getName

  def setOwner(owner: String): File = Files.setOwner(path, fileSystem.getUserPrincipalLookupService.lookupPrincipalByName(owner))

  def setGroup(group: String): File = Files.setOwner(path, fileSystem.getUserPrincipalLookupService.lookupPrincipalByGroupName(group))

  /**
   * Similar to the UNIX command touch - create this file if it does not exist and set its last modification time
   */
  def touch(time: Instant = Instant.now())(implicit attributes: File.Attributes = File.Attributes.default): File =
    Files.setLastModifiedTime(createIfNotExists()(attributes).path, FileTime.from(time))

  def lastModifiedTime(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Instant = Files.getLastModifiedTime(path, linkOptions: _*).toInstant

  /**
   * Deletes this file or directory
   * @param swallowIOExceptions If this is set to true, any exception thrown is swallowed
   */
  def delete(swallowIOExceptions: Boolean = false): File = returning(this) {
    try {
      if (isDirectory) list.foreach(_.delete(swallowIOExceptions))
      Files.delete(path)
    } catch {
      case e: IOException if swallowIOExceptions => //e.printStackTrace() //swallow
    }
  }

  def renameTo(newName: String): File = moveTo(path resolveSibling newName)

  /**
   *
   * @param destination
   * @param overwrite
   * @return destination
   */
  def moveTo(destination: File, overwrite: Boolean = false): File = Files.move(path, destination.path, File.CopyOptions(overwrite): _*)

  /**
   *
   * @param destination
   * @param overwrite
   * @return destination
   */
  def copyTo(destination: File, overwrite: Boolean = false): File = if(isDirectory) { //TODO: maxDepth?
    if (overwrite) destination.delete(swallowIOExceptions = true)
    Files.walkFileTree(path, new SimpleFileVisitor[Path] {
      def newPath(subPath: Path): Path = destination.path resolve (path relativize subPath)

      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = {
        Files.createDirectories(newPath(dir))
        super.preVisitDirectory(dir, attrs)
      }

      override def visitFile(file: Path, attrs: BasicFileAttributes) = {
        Files.copy(file, newPath(file), File.CopyOptions(overwrite): _*)
        super.visitFile(file, attrs)
      }
    })
    destination
  } else {
    Files.copy(path, destination.path, File.CopyOptions(overwrite): _*)
  }

  def symbolicLinkTo(destination: File)(implicit attributes: File.Attributes = File.Attributes.default) = Files.createSymbolicLink(path, destination.path, attributes: _*)

  def linkTo(destination: File, symbolic: Boolean = false)(implicit attributes: File.Attributes = File.Attributes.default): File =
    if (symbolic) symbolicLinkTo(destination)(attributes) else Files.createLink(path, destination.path)

  def listRelativePaths(implicit visitOptions: File.VisitOptions = File.VisitOptions.default): Iterator[Path] = walk()(visitOptions).map(relativize)

  def relativize(destination: File): Path = path relativize destination.path

  def isSamePathAs(that: File): Boolean = this.path == that.path

  def isSameFileAs(that: File): Boolean = Files.isSameFile(this.path, that.path)

  /**
   * @return true if this file is exactly same as that file
   *         For directories, it checks for equivalent directory structure
   */
  def isSameContentAs(that: File): Boolean = isSimilarContentAs(that)
  def `===`(that: File): Boolean = isSameContentAs(that)

  /**
   * Almost same as isSameContentAs but uses faster md5 hashing to compare (and thus small chance of false positive)
   * Also works for directories
   *
   * @param that
   * @return
   */
  def isSimilarContentAs(that: File): Boolean = this.md5 == that.md5

  def =!=(that: File): Boolean = !isSameContentAs(that)

  override def equals(obj: Any) = obj match {
    case file: File => isSamePathAs(file)
    case _ => false
  }

  /**
   * @return true if file is not present or empty directory or 0-bytes file
   */
  def isEmpty: Boolean = this match {
    case File.Type.Directory(children) => children.isEmpty
    case File.Type.RegularFile(content) => content.isEmpty
    case _ => notExists
  }

  /**
   * If this is a directory, remove all its children
   * If its a file, empty the contents
   * @return this
   */
  def clear(): File = this match {
    case File.Type.Directory(children) => children.foreach(_.delete()); this
    case _ => write(Array.ofDim[Byte](0))(File.OpenOptions.default)
  }

  override def hashCode = path.hashCode()

  override def toString = pathAsString

  /**
   * Zips this file (or directory)
   *
   * @param destination The destination file; Creates this if it does not exists
   * @return The destination zip file
   */
  def zipTo(destination: File, compressionLevel: Int = Deflater.DEFAULT_COMPRESSION)(implicit codec: Codec): File = {
    val files = if (isDirectory) children.toSeq else Seq(this)
    Cmds.zip(files: _*)(destination, compressionLevel)(codec)
  }

  /**
   * zip to a temp directory
   * @return the target directory
   */
  def zip(compressionLevel: Int = Deflater.DEFAULT_COMPRESSION)(implicit codec: Codec): File = zipTo(destination = File.newTemporaryFile(name, ".zip"), compressionLevel)(codec)

  /**
   * Unzips this zip file
   *
   * @param destination destination folder; Creates this if it does not exist
   * @return The destination where contents are unzipped
   */
  def unzipTo(destination: File)(implicit codec: Codec): File = returning(destination) {
    for {
      zipFile <- new ZipFile(toJava, codec).autoClosed
      entry <- zipFile.entries()
      file = destination.createChild(entry.getName, entry.isDirectory)
      if !entry.isDirectory
    } zipFile.getInputStream(entry) > file.newOutputStream
  }

  /**
   * unzip to a temporary zip file
   * @return the zip file
   */
  def unzip()(implicit codec: Codec): File = unzipTo(destination = File.newTemporaryDirectory(name))(codec)

  //TODO: add features from https://github.com/sbt/io/blob/master/io/src/main/scala/sbt/io/IO.scala
}

object File {
  def newTemporaryDirectory(prefix: String = "", parent: Option[File] = None)(implicit attributes: Attributes = Attributes.default): File =
    parent match {
      case Some(dir) => Files.createTempDirectory(dir.path, prefix, attributes: _*)
      case _ => Files.createTempDirectory(prefix, attributes: _*)
    }

  def newTemporaryFile(prefix: String = "", suffix: String = "", parent: Option[File] = None)(implicit attributes: Attributes = Attributes.default): File =
    parent match {
      case Some(dir) => Files.createTempFile(dir.path, prefix, suffix, attributes: _*)
      case _ => Files.createTempFile(prefix, suffix, attributes: _*)
    }

  implicit def apply(path: Path): File = new File(path.normalize().toAbsolutePath)

  def apply(path: String, fragments: String*): File = Paths.get(path, fragments: _*)

  def apply(uri: URI): File = Paths.get(uri)

  def roots: Iterable[File] = FileSystems.getDefault.getRootDirectories.map(File.apply)

  def root: File = roots.head

  def home: File = Properties.userHome.toFile

  def temp: File = Properties.tmpDir.toFile

  def currentWorkingDirectory: File = File("")

  type Attributes = Seq[FileAttribute[_]]
  object Attributes {
    val default   : Attributes = Seq.empty
  }

  type CopyOptions = Seq[StandardCopyOption]
  object CopyOptions {
    def apply(overwrite: Boolean) : CopyOptions = if (overwrite) StandardCopyOption.REPLACE_EXISTING +: default else default
    val default                   : CopyOptions = Seq.empty //Seq(StandardCopyOption.COPY_ATTRIBUTES)
  }

  type Events = Seq[WatchEvent.Kind[_]]
  object Events {
    val all     : Events = Seq(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE)
    val default : Events = all
  }

  type OpenOptions = Seq[OpenOption]
  object OpenOptions {
    val append  : OpenOptions = Seq(StandardOpenOption.APPEND, StandardOpenOption.CREATE)
    val default : OpenOptions = Seq.empty
  }

  type LinkOptions = Seq[LinkOption]
  object LinkOptions {
    val follow    : LinkOptions = Seq.empty
    val noFollow  : LinkOptions = Seq(LinkOption.NOFOLLOW_LINKS)
    val default   : LinkOptions = follow
  }

  type VisitOptions = Seq[FileVisitOption]
  object VisitOptions {
    val follow    : VisitOptions = Seq(FileVisitOption.FOLLOW_LINKS)
    val default   : VisitOptions = Seq.empty
  }

  type Order = Ordering[File]
  object Order {
    val bySize              : Order = Ordering.by(_.size)
    val byName              : Order = Ordering.by(_.name)
    val byDepth             : Order = Ordering.by(_.path.getNameCount)
    val byModificationTime  : Order = Ordering.by(_.lastModifiedTime)
    val byDirectoriesLast   : Order = Ordering.by(_.isDirectory)
    val byDirectoriesFirst  : Order = byDirectoriesLast.reverse
    val default             : Order = byDirectoriesFirst
  }

  /**
   * Denote various file types using this
   *
   * @tparam Content The type of underlying contents e.g. a directory has its children files as contents but a regular file may have bytes as contents
   */
  sealed trait Type[Content] {
    def unapply(file: File): Option[Content]
  }

  object Type {
    case object RegularFile extends Type[BufferedSource] {
      override def unapply(file: File) = when(file.isRegularFile)(file.newBufferedSource)
    }

    case object Directory extends Type[Files] {
      def unapply(file: File) = when(file.isDirectory)(file.children)
    }

    case object SymbolicLink extends Type[File] {
      def unapply(file: File) = file.symbolicLink
    }
  }

  class PathMatcherSyntax private (val name: String)
  object PathMatcherSyntax {
    val glob = new PathMatcherSyntax("glob")
    val regex = new PathMatcherSyntax("regex")
    val default = glob
    def other(syntax: String) = new PathMatcherSyntax(syntax)
  }

  class RandomAccessMode private(val value: String)
  object RandomAccessMode {
    val read = new RandomAccessMode("r")
    val readWrite = new RandomAccessMode("rw")
    val readWriteMetadataSynchronous = new RandomAccessMode("rws")
    val readWriteContentSynchronous = new RandomAccessMode("rwd")
  }

  def numberOfOpenFileDescriptors(): Long = java.lang.management.ManagementFactory.getOperatingSystemMXBean match {
    case os: com.sun.management.UnixOperatingSystemMXBean => os.getMaxFileDescriptorCount
    case os => throw new UnsupportedOperationException(s"Unsupported operating system: $os")
  }

  /**
   * Implement this interface to monitor the root file
   */
  trait Monitor {
    val root: File

    /**
     * Dispatch a StandardWatchEventKind to an appropriate callback
     * Override this if you don't want to manually handle onDelete/onCreate/onModify separately
     *
     * @param eventType
     * @param file
     */
    def onEvent(eventType: WatchEvent.Kind[Path], file: File): Unit = eventType match {
      case StandardWatchEventKinds.ENTRY_CREATE => onCreate(file)
      case StandardWatchEventKinds.ENTRY_MODIFY => onModify(file)
      case StandardWatchEventKinds.ENTRY_DELETE => onDelete(file)
    }

    def start(): Unit

    def onCreate(file: File): Unit

    def onModify(file: File): Unit

    def onDelete(file: File): Unit

    def onUnknownEvent(event: WatchEvent[_]): Unit

    def onException(exception: Throwable): Unit

    def stop(): Unit
  }
}
