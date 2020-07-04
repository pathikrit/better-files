package better.files

import java.io.{File => JFile, _}
import java.net.{URI, URL}
import java.nio.charset.Charset
import java.nio.channels._
import java.nio.file._
import java.nio.file.attribute._
import java.security.MessageDigest
import java.time.Instant
import java.util.function.BiPredicate
import java.util.regex.Pattern
import java.util.zip._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.util.Properties
import scala.util.matching.Regex

/** Scala wrapper around java.nio.files.Path */
@SerialVersionUID(3435L)
class File private (val path: Path)(implicit val fileSystem: FileSystem = path.getFileSystem) extends Serializable {
  //TODO: LinkOption?

  def pathAsString: String =
    path.toString

  /**
    * getResource[...](path) always uses "/" for separator
    *   https://docs.oracle.com/javase/8/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String)
    */
  def toResourcePathAsString: String =
    pathAsString.replace(JFile.separatorChar, '/')

  def toJava: JFile =
    new JFile(path.toAbsolutePath.toString)

  /**
    * Name of file
    * Certain files may not have a name e.g. root directory - returns empty string in that case
    *
    * @return
    */
  def name: String =
    nameOption.getOrElse("")

  /**
    * Certain files may not have a name e.g. root directory - returns None in that case
    *
    * @return
    */
  def nameOption: Option[String] =
    Option(path.getFileName).map(_.toString)

  def root: File =
    path.getRoot

  def canonicalPath: String =
    toJava.getAbsolutePath

  def canonicalFile: File =
    toJava.getCanonicalFile.toScala

  def nameWithoutExtension: String =
    nameWithoutExtension(includeAll = true)

  /**
    * @param includeAll
    *         For files with multiple extensions e.g. "bundle.tar.gz"
    *         nameWithoutExtension(includeAll = true) returns "bundle"
    *         nameWithoutExtension(includeAll = false) returns "bundle.tar"
    * @return
    */
  def nameWithoutExtension(includeAll: Boolean): String =
    if (hasExtension) name.substring(0, indexOfExtension(includeAll)) else name

  /**
    * @return extension (including the dot) of this file if it is a regular file and has an extension, else None
    */
  def extension: Option[String] =
    extension()

  /**
    * @param includeDot  whether the dot should be included in the extension or not
    * @param includeAll  whether all extension tokens should be included, or just the last one e.g. for bundle.tar.gz should it be .tar.gz or .gz
    * @param toLowerCase to lowercase the extension or not e.g. foo.HTML should have .html or .HTML
    * @return extension of this file if it is a regular file and has an extension, else None
    */
  def extension(includeDot: Boolean = true, includeAll: Boolean = false, toLowerCase: Boolean = true): Option[String] =
    when(hasExtension) {
      val dot       = indexOfExtension(includeAll)
      val index     = if (includeDot) dot else dot + 1
      val extension = name.substring(index)
      if (toLowerCase) extension.toLowerCase else extension
    }

  private[this] def indexOfExtension(includeAll: Boolean) =
    if (includeAll) name.indexOf(".") else name.lastIndexOf(".")

  /**
    * Returns the extension if file is a regular file
    * If file is unreadable or does not exist, it is assumed to be not a regular file
    * See: https://github.com/pathikrit/better-files/issues/89
    *
    * @return
    */
  def hasExtension: Boolean =
    (isRegularFile || notExists) && name.contains(".")

  /**
    * Changes the file-extension by renaming this file; if file does not have an extension, it adds the extension
    * Example usage file"foo.java".changeExtensionTo(".scala")
    *
    * If file does not exist (or is a directory) no change is done and the current file is returned
    */
  def changeExtensionTo(extension: String): File = {
    val newName = s"$nameWithoutExtension.${extension.stripPrefix(".")}"
    if (isRegularFile) renameTo(newName) else if (notExists) File(newName) else this
  }

  def contentType: Option[String] =
    Option(Files.probeContentType(path))

  /**
    * Return parent of this file
    * NOTE: This API returns null if this file is the root;
    * please use parentOption if you expect to handle roots
    *
    * @see parentOption
    * @return
    */
  def parent: File =
    parentOption.orNull

  /**
    *
    * @return Some(parent) of this file or None if this is the root and thus has no parent
    */
  def parentOption: Option[File] =
    Option(path.getParent).map(File.apply)

  def /(child: String): File =
    path.resolve(child)

  def /(child: Symbol): File =
    this / child.name

  def createChild(
      child: String,
      asDirectory: Boolean = false,
      createParents: Boolean = false
  )(implicit
      attributes: File.Attributes = File.Attributes.default,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): File =
    (this / child).createIfNotExists(asDirectory, createParents)(attributes, linkOptions)

  /**
    * Create this file. If it exists, don't do anything
    *
    * @param asDirectory   If you want this file to be created as a directory instead, set this to true (false by default)
    * @param createParents If you also want all the parents to be created from root to this file (false by default)
    * @param attributes
    * @param linkOptions
    * @return
    */
  def createIfNotExists(
      asDirectory: Boolean = false,
      createParents: Boolean = false
  )(implicit
      attributes: File.Attributes = File.Attributes.default,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): this.type = {
    if (exists(linkOptions)) {
      this
    } else if (asDirectory) {
      createDirectories()(attributes)
    } else {
      if (createParents) parent.createDirectories()(attributes)
      try {
        createFile()(attributes)
      } catch {
        case _: FileAlreadyExistsException if isRegularFile(linkOptions) => // We don't really care if it exists already
      }
      this
    }
  }

  def createFileIfNotExists(
      createParents: Boolean = false
  )(implicit
      attributes: File.Attributes = File.Attributes.default,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): this.type =
    createIfNotExists(asDirectory = false, createParents = createParents)

  def createDirectoryIfNotExists(
      createParents: Boolean = false
  )(implicit
      attributes: File.Attributes = File.Attributes.default,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): this.type =
    createIfNotExists(asDirectory = true, createParents = createParents)

  /**
    * Create this file
    *
    * @param attributes
    * @return
    */
  def createFile()(implicit attributes: File.Attributes = File.Attributes.default): this.type = {
    Files.createFile(path, attributes: _*)
    this
  }

  def exists(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    Files.exists(path, linkOptions: _*)

  def notExists(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    Files.notExists(path, linkOptions: _*)

  def sibling(name: String): File =
    path.resolveSibling(name)

  def isSiblingOf(sibling: File): Boolean =
    sibling.isChildOf(parent)

  def siblings: Iterator[File] =
    parent.list.filterNot(_ == this)

  def isChildOf(parent: File): Boolean =
    parent.isParentOf(this)

  /**
    * Check if this directory contains this file
    *
    * @param file
    * @param strict If strict is false, it would return true for self.contains(self)
    * @return true if this is a directory and it contains this file
    */
  def contains(file: File, strict: Boolean = true): Boolean =
    isDirectory && (file.path startsWith path) && (!strict || !isSamePathAs(file))

  def isParentOf(child: File): Boolean =
    contains(child)

  def bytes: Iterator[Byte] =
    newInputStream.buffered.bytes //TODO: Dispose here?

  def loadBytes: Array[Byte] =
    Files.readAllBytes(path)

  def byteArray: Array[Byte] =
    loadBytes

  /**
    * Create this directory
    *
    * @param attributes
    * @return
    */
  def createDirectory()(implicit attributes: File.Attributes = File.Attributes.default): this.type = {
    Files.createDirectory(path, attributes: _*)
    this
  }

  /**
    * Create this directory and all its parents
    * Unlike the JDK, this by default sanely handles the JDK-8130464 bug
    * If you want default Java behaviour, use File.LinkOptions.noFollow
    *
    * @param attributes
    * @return
    */
  def createDirectories(
  )(implicit
      attributes: File.Attributes = File.Attributes.default,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): this.type = {
    try {
      Files.createDirectories(path, attributes: _*)
    } catch {
      case _: FileAlreadyExistsException if isDirectory(linkOptions) => // work around for JDK-8130464
    }
    this
  }

  def chars(implicit charset: Charset = DefaultCharset): Iterator[Char] =
    newBufferedReader(charset).chars //TODO: Dispose here?

  /**
    * Load all lines from this file
    * Note: Large files may cause an OutOfMemory in which case, use the streaming version @see lineIterator
    *
    * @param charset
    * @return all lines in this file
    */
  def lines(implicit charset: Charset = DefaultCharset): Traversable[String] =
    Files.readAllLines(path, charset).asScala

  def lineCount(implicit charset: Charset = DefaultCharset): Long =
    Files.lines(path, charset).count()

  /**
    * Iterate over lines in a file (auto-close stream on complete)
    * NOTE: If the iteration is partial, it may leave a stream open
    * If you want partial iteration use @see lines()
    *
    * @param charset
    * @return
    */
  def lineIterator(implicit charset: Charset = DefaultCharset): Iterator[String] =
    Files.lines(path, charset).toAutoClosedIterator

  def tokens(
      splitter: StringSplitter = StringSplitter.Default
  )(implicit
      charset: Charset = DefaultCharset
  ): Iterator[String] =
    newBufferedReader(charset).tokens(splitter)

  def contentAsString(implicit charset: Charset = DefaultCharset): String =
    new String(byteArray, charset)

  def printLines(
      lines: TraversableOnce[_]
  )(implicit
      openOptions: File.OpenOptions = File.OpenOptions.append
  ): this.type = {
    printWriter()(openOptions).foreach(_.printLines(lines))
    this
  }

  /**
    * For large number of lines that may not fit in memory, use printLines
    *
    * @param lines
    * @param charset
    * @return
    */
  def appendLines(lines: String*)(implicit charset: Charset = DefaultCharset): this.type = {
    Files.write(path, lines.asJava, charset, File.OpenOptions.append: _*)
    this
  }

  def appendLine(line: String = "")(implicit charset: Charset = DefaultCharset): this.type =
    appendLines(line)(charset)

  def append(text: String)(implicit charset: Charset = DefaultCharset): this.type =
    appendByteArray(text.getBytes(charset))

  def appendText(text: String)(implicit charset: Charset = DefaultCharset): this.type =
    append(text)(charset)

  def appendByteArray(bytes: Array[Byte]): this.type = {
    Files.write(path, bytes, File.OpenOptions.append: _*)
    this
  }

  def appendBytes(bytes: Iterator[Byte]): this.type =
    writeBytes(bytes)(openOptions = File.OpenOptions.append)

  /**
    * Write byte array to file. For large contents consider using the writeBytes
    *
    * @param bytes
    * @return this
    */
  def writeByteArray(
      bytes: Array[Byte]
  )(implicit
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): this.type = {
    Files.write(path, bytes, openOptions: _*)
    this
  }

  def writeBytes(
      bytes: Iterator[Byte]
  )(implicit
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): this.type = {
    outputStream(openOptions).foreach(_.buffered.write(bytes))
    this
  }

  def write(
      text: String
  )(implicit
      openOptions: File.OpenOptions = File.OpenOptions.default,
      charset: Charset = DefaultCharset
  ): this.type =
    writeByteArray(text.getBytes(charset))(openOptions)

  def writeText(
      text: String
  )(implicit
      openOptions: File.OpenOptions = File.OpenOptions.default,
      charset: Charset = DefaultCharset
  ): this.type =
    write(text)(openOptions, charset)

  def overwrite(
      text: String
  )(implicit
      openOptions: File.OpenOptions = File.OpenOptions.default,
      charset: Charset = DefaultCharset
  ): this.type =
    write(text)(openOptions, charset)

  def newRandomAccess(mode: File.RandomAccessMode = File.RandomAccessMode.read): RandomAccessFile =
    new RandomAccessFile(toJava, mode.value)

  def randomAccess(mode: File.RandomAccessMode = File.RandomAccessMode.read): Dispose[RandomAccessFile] =
    newRandomAccess(mode).autoClosed //TODO: Mode enum?

  def newBufferedReader(implicit charset: Charset = DefaultCharset): BufferedReader =
    Files.newBufferedReader(path, charset)

  def bufferedReader(implicit charset: Charset = DefaultCharset): Dispose[BufferedReader] =
    newBufferedReader(charset).autoClosed

  def newBufferedWriter(implicit
      charset: Charset = DefaultCharset,
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): BufferedWriter =
    Files.newBufferedWriter(path, charset, openOptions: _*)

  def bufferedWriter(implicit
      charset: Charset = DefaultCharset,
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): Dispose[BufferedWriter] =
    newBufferedWriter(charset, openOptions).autoClosed

  def newFileReader: FileReader =
    new FileReader(toJava)

  def fileReader: Dispose[FileReader] =
    newFileReader.autoClosed

  def newFileWriter(append: Boolean = false): FileWriter =
    new FileWriter(toJava, append)

  def fileWriter(append: Boolean = false): Dispose[FileWriter] =
    newFileWriter(append).autoClosed

  def newPrintWriter(
      autoFlush: Boolean = false
  )(implicit
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): PrintWriter =
    new PrintWriter(newOutputStream(openOptions), autoFlush)

  def printWriter(
      autoFlush: Boolean = false
  )(implicit
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): Dispose[PrintWriter] =
    newPrintWriter(autoFlush)(openOptions).autoClosed

  def newInputStream(implicit openOptions: File.OpenOptions = File.OpenOptions.default): InputStream =
    Files.newInputStream(path, openOptions: _*)

  def inputStream(implicit openOptions: File.OpenOptions = File.OpenOptions.default): Dispose[InputStream] =
    newInputStream(openOptions).autoClosed

  def newFileInputStream: FileInputStream =
    new FileInputStream(toJava)

  def fileInputStream: Dispose[FileInputStream] =
    newFileInputStream.autoClosed

  def newFileOutputStream(append: Boolean = false): FileOutputStream =
    new FileOutputStream(toJava, append)

  def fileOutputStream(append: Boolean = false): Dispose[FileOutputStream] =
    newFileOutputStream(append).autoClosed

  def newScanner(
      splitter: StringSplitter = StringSplitter.Default
  )(implicit
      charset: Charset = DefaultCharset
  ): Scanner =
    Scanner(newBufferedReader(charset), splitter)

  def scanner(
      splitter: StringSplitter = StringSplitter.Default
  )(implicit
      charset: Charset = DefaultCharset
  ): Dispose[Scanner] =
    newScanner(splitter)(charset).autoClosed

  def newOutputStream(implicit openOptions: File.OpenOptions = File.OpenOptions.default): OutputStream =
    Files.newOutputStream(path, openOptions: _*)

  def outputStream(implicit openOptions: File.OpenOptions = File.OpenOptions.default): Dispose[OutputStream] =
    newOutputStream(openOptions).autoClosed

  def newZipOutputStream(implicit
      openOptions: File.OpenOptions = File.OpenOptions.default,
      charset: Charset = DefaultCharset
  ): ZipOutputStream =
    new ZipOutputStream(newOutputStream(openOptions), charset)

  def zipInputStream(implicit charset: Charset = DefaultCharset): Dispose[ZipInputStream] =
    newZipInputStream(charset).autoClosed

  def newZipInputStream(implicit charset: Charset = DefaultCharset): ZipInputStream =
    new ZipInputStream(newFileInputStream.buffered, charset)

  def zipOutputStream(implicit
      openOptions: File.OpenOptions = File.OpenOptions.default,
      charset: Charset = DefaultCharset
  ): Dispose[ZipOutputStream] =
    newZipOutputStream(openOptions, charset).autoClosed

  def newGzipOutputStream(
      bufferSize: Int = DefaultBufferSize,
      syncFlush: Boolean = false,
      append: Boolean = false
  ): GZIPOutputStream =
    new GZIPOutputStream(newFileOutputStream(append), bufferSize, syncFlush)

  def gzipOutputStream(
      bufferSize: Int = DefaultBufferSize,
      syncFlush: Boolean = false,
      append: Boolean = false
  ): Dispose[GZIPOutputStream] =
    newGzipOutputStream(bufferSize = bufferSize, syncFlush = syncFlush, append = append).autoClosed

  def newGzipInputStream(bufferSize: Int = DefaultBufferSize): GZIPInputStream =
    new GZIPInputStream(newFileInputStream, bufferSize)

  def gzipInputStream(bufferSize: Int = DefaultBufferSize): Dispose[GZIPInputStream] =
    newGzipInputStream(bufferSize).autoClosed

  def newFileChannel(implicit
      openOptions: File.OpenOptions = File.OpenOptions.default,
      attributes: File.Attributes = File.Attributes.default
  ): FileChannel =
    FileChannel.open(path, openOptions.toSet.asJava, attributes: _*)

  def fileChannel(implicit
      openOptions: File.OpenOptions = File.OpenOptions.default,
      attributes: File.Attributes = File.Attributes.default
  ): Dispose[FileChannel] =
    newFileChannel(openOptions, attributes).autoClosed

  def newAsynchronousFileChannel(implicit
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): AsynchronousFileChannel =
    AsynchronousFileChannel.open(path, openOptions: _*)

  def asynchronousFileChannel(implicit
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): Dispose[AsynchronousFileChannel] =
    newAsynchronousFileChannel(openOptions).autoClosed

  def newWatchService: WatchService =
    fileSystem.newWatchService()

  def watchService: Dispose[WatchService] =
    newWatchService.autoClosed

  /**
    * Serialize a object using Java's serializer into this file, creating it and its parents if they do not exist
    *
    * @param obj
    * @return
    */
  def writeSerialized(
      obj: Serializable,
      bufferSize: Int = DefaultBufferSize
  )(implicit
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): this.type = {
    createFileIfNotExists(createParents = true)
      .outputStream(openOptions)
      .foreach(_.asObjectOutputStream(bufferSize).serialize(obj).flush())
    this
  }

  /**
    * Deserialize a object using Java's default serialization from this file
    *
    * @return
    */
  def readDeserialized[A](
      classLoaderOverride: Option[ClassLoader] = None,
      bufferSize: Int = DefaultBufferSize
  )(implicit
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): A =
    classLoaderOverride match {
      case Some(classLoader) =>
        inputStream(openOptions).apply(_.asObjectInputStreamUsingClassLoader(classLoader, bufferSize).deserialize[A])
      case _ => inputStream(openOptions).apply(_.asObjectInputStream(bufferSize).deserialize[A])
    }

  def register(service: WatchService, events: File.Events = File.Events.all): this.type = {
    path.register(service, events.toArray)
    this
  }

  def digest(algorithm: MessageDigest): Array[Byte] = {
    listRelativePaths.toSeq.sorted foreach { relativePath =>
      val file: File = path.resolve(relativePath)
      if (file.isDirectory) {
        algorithm.update(relativePath.toString.getBytes)
      } else {
        file.newInputStream().withMessageDigest(algorithm).autoClosed.foreach(_.pipeTo(NullOutputStream))
      }
    }
    algorithm.digest()
  }

  /**
    * Set a file attribute e.g. file("dos:system") = true
    *
    * @param attribute
    * @param value
    * @param linkOptions
    * @return
    */
  def update(
      attribute: String,
      value: Any
  )(implicit
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): this.type = {
    Files.setAttribute(path, attribute, value, linkOptions: _*)
    this
  }

  /**
    * @return checksum of this file (or directory) in hex format
    */
  def checksum(algorithm: MessageDigest): String =
    toHex(digest(algorithm))

  def md5: String =
    checksum("MD5")

  def sha1: String =
    checksum("SHA-1")

  def sha256: String =
    checksum("SHA-256")

  def sha512: String =
    checksum("SHA-512")

  /**
    * @return Some(target) if this is a symbolic link (to target) else None
    */
  def symbolicLink: Option[File] =
    when(isSymbolicLink)(new File(Files.readSymbolicLink(path)))

  /**
    * @return true if this file (or the file found by following symlink) is a directory
    */
  def isDirectory(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    Files.isDirectory(path, linkOptions: _*)

  /**
    * @return true if this file (or the file found by following symlink) is a regular file
    */
  def isRegularFile(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    Files.isRegularFile(path, linkOptions: _*)

  def isSymbolicLink: Boolean =
    Files.isSymbolicLink(path)

  def isHidden: Boolean =
    Files.isHidden(path)

  /**
    * List files recursively up to given depth using a custom file filter
    *
    * @param filter
    * @param maxDepth
    * @param visitOptions
    * @return
    */
  def list(
      filter: File => Boolean,
      maxDepth: Int = Int.MaxValue,
      visitOptions: File.VisitOptions = File.VisitOptions.default
  ): Iterator[File] = {
    val predicate = new BiPredicate[Path, BasicFileAttributes] {
      override def test(p: Path, a: BasicFileAttributes) = filter(p)
    }
    Files.find(path, maxDepth, predicate, visitOptions: _*)
  }

  /**
    * Check if a file is locked.
    *
    * @param mode     The random access mode.
    * @param position The position at which the locked region is to start; must be non-negative.
    * @param size     The size of the locked region; must be non-negative, and the sum position + size must be non-negative.
    * @param isShared true to request a shared lock, false to request an exclusive lock.
    * @return True if the file is locked, false otherwise.
    */
  def isLocked(
      mode: File.RandomAccessMode,
      position: Long = 0L,
      size: Long = Long.MaxValue,
      isShared: Boolean = false
  )(implicit
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): Boolean =
    try {
      usingLock(mode) { channel =>
        channel.tryLock(position, size, isShared).release()
        false
      }
    } catch {
      case _: OverlappingFileLockException | _: NonWritableChannelException | _: NonReadableChannelException => true

      // Windows throws a `FileNotFoundException` if the file is locked (see: https://github.com/pathikrit/better-files/pull/194)
      case _: FileNotFoundException if verifiedExists(linkOptions).getOrElse(true) => true
    }

  /**
    * @see https://docs.oracle.com/javase/tutorial/essential/io/check.html
    * @see https://stackoverflow.com/questions/30520179/why-does-file-exists-return-true-even-though-files-exists-in-the-nio-files
    *
    * @return
    *         Some(true) if file is guaranteed to exist
    *         Some(false) if file is guaranteed to not exist
    *         None if the status is unknown e.g. if file is unreadable
    */
  def verifiedExists(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Option[Boolean] = {
    if (exists(linkOptions)) {
      Some(true)
    } else if (notExists(linkOptions)) {
      Some(false)
    } else {
      None
    }
  }

  def usingLock[U](mode: File.RandomAccessMode)(f: FileChannel => U): U =
    newRandomAccess(mode).getChannel.autoClosed.apply(f)

  def isReadLocked(position: Long = 0L, size: Long = Long.MaxValue, isShared: Boolean = false) =
    isLocked(File.RandomAccessMode.read, position, size, isShared)

  def isWriteLocked(position: Long = 0L, size: Long = Long.MaxValue, isShared: Boolean = false) =
    isLocked(File.RandomAccessMode.readWrite, position, size, isShared)

  def list: Iterator[File] =
    Files.list(path)

  def children: Iterator[File] = list

  def entries: Iterator[File] = list

  def listRecursively(implicit visitOptions: File.VisitOptions = File.VisitOptions.default): Iterator[File] =
    walk()(visitOptions).filterNot(isSamePathAs)

  /**
    * Walk the directory tree recursively upto maxDepth
    *
    * @param maxDepth
    * @return List of children in BFS maxDepth level deep (includes self since self is at depth = 0)
    */
  def walk(
      maxDepth: Int = Int.MaxValue
  )(implicit
      visitOptions: File.VisitOptions = File.VisitOptions.default
  ): Iterator[File] =
    Files.walk(path, maxDepth, visitOptions: _*) //TODO: that ignores I/O errors?

  def pathMatcher(syntax: File.PathMatcherSyntax, includePath: Boolean)(pattern: String): PathMatcher =
    syntax(this, pattern, includePath)

  /**
    * Util to glob from this file's path
    *
    *
    * @param includePath If true, we don't need to set path glob patterns
    *                    e.g. instead of **/ /*.txt we just use *.txt
   * @param maxDepth Recurse up to maxDepth
   * @return Set of files that matched
   */
  //TODO: Consider removing `syntax` as implicit. You often want to control this on a per method call basis
  def glob(
      pattern: String,
      includePath: Boolean = true,
      maxDepth: Int = Int.MaxValue
  )(implicit
      syntax: File.PathMatcherSyntax = File.PathMatcherSyntax.default,
      visitOptions: File.VisitOptions = File.VisitOptions.default
  ): Iterator[File] =
    pathMatcher(syntax, includePath)(pattern).matches(this, maxDepth)(visitOptions)

  /**
    * Util to match from this file's path using Regex
    *
    * @param includePath If true, we don't need to set path glob patterns
    *                    e.g. instead of **/ /*.txt we just use *.txt
   * @param maxDepth Recurse up to maxDepth
   * @see glob
   * @return Set of files that matched
   */
  def globRegex(
      pattern: Regex,
      includePath: Boolean = true,
      maxDepth: Int = Int.MaxValue
  )(implicit
      visitOptions: File.VisitOptions = File.VisitOptions.default
  ): Iterator[File] =
    glob(pattern.regex, includePath, maxDepth)(syntax = File.PathMatcherSyntax.regex, visitOptions = visitOptions)

  /**
    * More Scala friendly way of doing Files.walk
    * Note: This is lazy (returns an Iterator) and won't evaluate till we reify the iterator (e.g. using .toList)
    *
    * @param matchFilter
    * @param maxDepth
    * @return
    */
  def collectChildren(
      matchFilter: File => Boolean,
      maxDepth: Int = Int.MaxValue
  )(implicit
      visitOptions: File.VisitOptions = File.VisitOptions.default
  ): Iterator[File] =
    walk(maxDepth)(visitOptions).filter(matchFilter)

  def uri: URI =
    path.toUri

  def url: URL =
    uri.toURL

  /**
    * @return file size (for directories, return size of the directory) in bytes
    */
  def size(implicit visitOptions: File.VisitOptions = File.VisitOptions.default): Long =
    size(returnZeroIfMissing = isDirectory)

  /**
    * @param returnZeroIfMissing If true, return zeroes for missing files*
    * @return file size (for directories, return size of the directory) in bytes
    */
  def size(returnZeroIfMissing: Boolean)(implicit visitOptions: File.VisitOptions): Long =
    walk()(visitOptions)
      .map({ f =>
        try {
          Files.size(f.path)
        } catch {
          case _: FileNotFoundException if returnZeroIfMissing => 0L
        }
      })
      .sum

  def permissions(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Set[PosixFilePermission] =
    Files.getPosixFilePermissions(path, linkOptions: _*).asScala.toSet

  def permissionsAsString(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): String =
    PosixFilePermissions.toString(permissions(linkOptions).asJava)

  def setPermissions(permissions: Set[PosixFilePermission]): this.type = {
    Files.setPosixFilePermissions(path, permissions.asJava)
    this
  }

  def addPermission(
      permission: PosixFilePermission
  )(implicit
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): this.type =
    setPermissions(permissions(linkOptions) + permission)

  def removePermission(
      permission: PosixFilePermission
  )(implicit
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): this.type =
    setPermissions(permissions(linkOptions) - permission)

  /**
    * test if file has this permission
    */
  def testPermission(
      permission: PosixFilePermission
  )(implicit
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): Boolean =
    permissions(linkOptions)(permission)

  def isOwnerReadable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.OWNER_READ)(linkOptions)

  def isOwnerWritable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.OWNER_WRITE)(linkOptions)

  def isOwnerExecutable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.OWNER_EXECUTE)(linkOptions)

  def isGroupReadable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.GROUP_READ)(linkOptions)

  def isGroupWritable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.GROUP_WRITE)(linkOptions)

  def isGroupExecutable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.GROUP_EXECUTE)(linkOptions)

  def isOthersReadable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.OTHERS_READ)(linkOptions)

  def isOthersWritable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.OTHERS_WRITE)(linkOptions)

  def isOthersExecutable(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.OTHERS_EXECUTE)(linkOptions)

  /**
    * This differs from the above as this checks if the JVM can read this file even though the OS cannot in certain platforms
    *
    * @see isOwnerReadable
    * @return
    */
  def isReadable: Boolean =
    toJava.canRead

  def isWritable: Boolean =
    toJava.canWrite

  def isExecutable: Boolean =
    toJava.canExecute

  def attributes(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): BasicFileAttributes =
    Files.readAttributes(path, classOf[BasicFileAttributes], linkOptions: _*)

  def posixAttributes(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): PosixFileAttributes =
    Files.readAttributes(path, classOf[PosixFileAttributes], linkOptions: _*)

  def dosAttributes(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): DosFileAttributes =
    Files.readAttributes(path, classOf[DosFileAttributes], linkOptions: _*)

  def owner(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): UserPrincipal =
    Files.getOwner(path, linkOptions: _*)

  def ownerName(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): String =
    owner(linkOptions).getName

  def group(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): GroupPrincipal =
    posixAttributes(linkOptions).group()

  def groupName(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): String =
    group(linkOptions).getName

  def setOwner(owner: String): this.type = {
    Files.setOwner(path, fileSystem.getUserPrincipalLookupService.lookupPrincipalByName(owner))
    this
  }

  def setGroup(group: String)(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): this.type = {
    Files
      .getFileAttributeView(path, classOf[PosixFileAttributeView], linkOptions: _*)
      .setGroup(fileSystem.getUserPrincipalLookupService.lookupPrincipalByGroupName(group))
    this
  }

  /**
    * Similar to the UNIX command touch - create this file if it does not exist and set its last modification time
    */
  def touch(
      time: Instant = Instant.now()
  )(implicit
      attributes: File.Attributes = File.Attributes.default,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): this.type = {
    Files.setLastModifiedTime(createFileIfNotExists()(attributes, linkOptions).path, FileTime.from(time))
    this
  }

  def lastModifiedTime(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Instant =
    Files.getLastModifiedTime(path, linkOptions: _*).toInstant

  /**
    * Deletes this file or directory
    * Unless otherwise specified, this does not follow symlinks
    * i.e. if this is a symlink, only the symlink itself is deleted and not the linked object
    *
    * @param swallowIOExceptions If this is set to true, any exception thrown is swallowed
    */
  def delete(
      swallowIOExceptions: Boolean = false,
      linkOption: File.LinkOptions = File.LinkOptions.noFollow
  ): this.type = {
    try {
      // Note: We call .toList to exhaust the iterator upfront
      // since otherwise we wait until the last element of the iterator to close the underlying Stream
      // which when doing a DFS may lead to a lot of open file handles for large directories
      if (isDirectory(linkOption)) list.toList.foreach(_.delete(swallowIOExceptions, linkOption))
      Files.delete(path)
    } catch {
      case _: IOException if swallowIOExceptions => //e.printStackTrace() //swallow
    }
    this
  }

  def renameTo(newName: String): File =
    moveTo(path.resolveSibling(newName))

  /**
    *
    * @param destination
    * @return destination
    */
  def moveTo(
      destination: File
  )(implicit
      copyOptions: File.CopyOptions = File.CopyOptions(overwrite = false)
  ): destination.type = {
    Files.move(path, destination.path, copyOptions: _*)
    destination
  }

  /**
    * Moves this file into the given directory
    * @param directory
    *
    * @return the File referencing the new file created under destination
    */
  def moveToDirectory(directory: File)(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): File = {
    require(directory.isDirectory(linkOptions), s"$directory must be a directory")
    moveTo(directory / this.name)
  }

  /**
    *
    * @param destination
    * @param overwrite
    * @return destination
    */
  def copyTo(
      destination: File,
      overwrite: Boolean = false
  )(implicit
      copyOptions: File.CopyOptions = File.CopyOptions(overwrite)
  ): destination.type = {
    if (isDirectory) { //TODO: maxDepth?
      Files.walkFileTree(
        path,
        new SimpleFileVisitor[Path] {
          def newPath(subPath: Path): Path = destination.path.resolve(path.relativize(subPath))

          override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = {
            Files.createDirectories(newPath(dir))
            super.preVisitDirectory(dir, attrs)
          }

          override def visitFile(file: Path, attrs: BasicFileAttributes) = {
            Files.copy(file, newPath(file), copyOptions: _*)
            super.visitFile(file, attrs)
          }
        }
      )
    } else {
      Files.copy(path, destination.path, copyOptions: _*)
    }
    destination
  }

  /**
    * Copies this file into the given directory
    * @param directory
    *
    * @return the File referencing the new file created under destination
    */
  def copyToDirectory(
      directory: File
  )(implicit
      linkOptions: File.LinkOptions = File.LinkOptions.default,
      copyOptions: File.CopyOptions = File.CopyOptions.default
  ): File = {
    require(directory.isDirectory(linkOptions), s"$directory must be a directory")
    copyTo(directory / this.name)(copyOptions)
  }

  def symbolicLinkTo(
      destination: File
  )(implicit
      attributes: File.Attributes = File.Attributes.default
  ): destination.type = {
    Files.createSymbolicLink(path, destination.path, attributes: _*)
    destination
  }

  def linkTo(
      destination: File,
      symbolic: Boolean = false
  )(implicit
      attributes: File.Attributes = File.Attributes.default
  ): destination.type = {
    if (symbolic) {
      symbolicLinkTo(destination)(attributes)
    } else {
      Files.createLink(destination.path, path)
      destination
    }
  }

  def listRelativePaths(implicit visitOptions: File.VisitOptions = File.VisitOptions.default): Iterator[Path] =
    walk()(visitOptions).map(relativize)

  def relativize(destination: File): Path =
    path.relativize(destination.path)

  def isSamePathAs(that: File): Boolean =
    this.path == that.path

  def isSameFileAs(that: File): Boolean =
    Files.isSameFile(this.path, that.path)

  /**
    * @return true if this file is exactly same as that file
    *         For directories, it checks for equivalent directory structure
    */
  def isSameContentAs(that: File): Boolean =
    isSimilarContentAs(that)

  /**
    * Almost same as isSameContentAs but uses faster md5 hashing to compare (and thus small chance of false positive)
    * Also works for directories
    *
    * @param that
    * @return
    */
  def isSimilarContentAs(that: File): Boolean =
    this.md5 == that.md5

  override def equals(obj: Any) = {
    obj match {
      case file: File => isSamePathAs(file)
      case _          => false
    }
  }

  /**
    * @param linkOptions
    * @return true if file is not present or empty directory or 0-bytes file
    */
  def isEmpty(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean = {
    if (isDirectory(linkOptions)) {
      Files.list(path).autoClosed(_.count()) == 0 //Do not use children.isEmpty as it may leave stream open
    } else if (isRegularFile(linkOptions)) {
      toJava.length() == 0
    } else {
      notExists(linkOptions)
    }
  }

  /**
    *
    * @param linkOptions
    * @return for directories, true if it has no children, false otherwise
    *         for files, true if it is a 0-byte file, false otherwise
    *         else true if it exists, false otherwise
    */
  def nonEmpty(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    !isEmpty(linkOptions)

  /**
    * If this is a directory, remove all its children
    * If its a file, empty the contents
    *
    * @return this
    */
  def clear()(implicit linkOptions: File.LinkOptions = File.LinkOptions.default): this.type = {
    if (isDirectory(linkOptions)) {
      children.foreach(_.delete())
    } else {
      writeByteArray(Array.emptyByteArray)(File.OpenOptions.default)
    }
    this
  }

  def deleteOnExit(
      swallowIOExceptions: Boolean = false,
      linkOption: File.LinkOptions = File.LinkOptions.noFollow
  ): this.type = {
    try {
      if (isDirectory(linkOption)) list.toList.foreach(_.deleteOnExit(swallowIOExceptions, linkOption))
      toJava.deleteOnExit()
    } catch {
      case _: IOException if swallowIOExceptions => //e.printStackTrace() //swallow
    }
    this
  }

  override def hashCode =
    path.hashCode()

  override def toString =
    pathAsString

  /**
    * Zips this file (or directory)
    *
    * @param destination The destination file; Creates this if it does not exists
    * @return The destination zip file
    */
  def zipTo(
      destination: File,
      compressionLevel: Int = Deflater.DEFAULT_COMPRESSION
  )(implicit
      charset: Charset = DefaultCharset
  ): destination.type = {
    val files = if (isDirectory) children else Iterator(this)
    destination.zipIn(files, compressionLevel)(charset)
  }

  /**
    * zip to a temp directory
    *
    * @return the target directory
    */
  def zip(compressionLevel: Int = Deflater.DEFAULT_COMPRESSION)(implicit charset: Charset = DefaultCharset): File =
    zipTo(destination = File.newTemporaryFile(prefix = name, suffix = ".zip"), compressionLevel)(charset)

  /**
    * Unzips this zip file
    *
    * @param destination destination folder; Creates this if it does not exist
    * @param zipFilter An optional param to reject or accept unzipping a file
    * @return The destination where contents are unzipped
    */
  def unzipTo(
      destination: File = File.newTemporaryDirectory(name.stripSuffix(".zip")),
      zipFilter: ZipEntry => Boolean = _ => true
  )(implicit
      charset: Charset = DefaultCharset
  ): destination.type = {
    for {
      zipFile <- new ZipFile(toJava, charset).autoClosed
      entry   <- zipFile.entries().asScala if zipFilter(entry)
    } entry.extractTo(destination, zipFile.getInputStream(entry))
    destination
  }

  /**
    * Streamed unzipping is slightly slower but supports larger files and more encodings
    * @see https://github.com/pathikrit/better-files/issues/152
    *
    * @param destinationDirectory destination folder; Creates this if it does not exist
    * @return The destination where contents are unzipped
    */
  def streamedUnzip(
      destinationDirectory: File = File.newTemporaryDirectory(name.stripSuffix(".zip"))
  )(implicit
      charset: Charset = DefaultCharset
  ): destinationDirectory.type = {
    for {
      zipIn <- zipInputStream(charset)
    } zipIn.mapEntries(_.extractTo(destinationDirectory, zipIn)).size
    destinationDirectory
  }

  def unGzipTo(
      destination: File = File.newTemporaryFile(suffix = name.stripSuffix(".gz")),
      append: Boolean = false,
      bufferSize: Int = DefaultBufferSize
  ): destination.type = {
    for {
      in  <- gzipInputStream(bufferSize)
      out <- destination.createFileIfNotExists(createParents = true).fileOutputStream(append)
    } in.pipeTo(out, bufferSize)
    destination
  }

  /**
    *
    * @param destination
    * @return
    */
  def gzipTo(
      destination: File = File.newTemporaryFile(suffix = name + ".gz"),
      bufferSize: Int = DefaultBufferSize,
      syncFlush: Boolean = false,
      append: Boolean = false
  ): destination.type = {
    for {
      in <- fileInputStream
      out <-
        destination
          .createFileIfNotExists(createParents = true)
          .gzipOutputStream(bufferSize = bufferSize, syncFlush = syncFlush, append = append)
    } in.buffered(bufferSize).pipeTo(out, bufferSize)
    destination
  }

  /**
    * Adds these files into this zip file
    * Example usage: File("test.zip").zipIn(Seq(file"hello.txt", file"hello2.txt"))
    *
    * @param files
    * @param compressionLevel
    * @param charset
    * @return this
    */
  def zipIn(
      files: Iterator[File],
      compressionLevel: Int = Deflater.DEFAULT_COMPRESSION
  )(implicit
      charset: Charset = DefaultCharset
  ): this.type = {
    for {
      output <- newZipOutputStream(File.OpenOptions.default, charset).withCompressionLevel(compressionLevel).autoClosed
      input  <- files
      file   <- input.walk()
      name = input.parent.relativize(file)
    } output.add(file, name.toString)
    this
  }

  /**
    * unzip to a temporary zip file
    *
    * @return the zip file
    */
  def unzip(zipFilter: ZipEntry => Boolean = _ => true)(implicit charset: Charset = DefaultCharset): File =
    unzipTo(destination = File.newTemporaryDirectory(name.stripSuffix(".zip")), zipFilter)(charset)

  /**
    * Java's temporary files/directories are not cleaned up by default.
    * If we explicitly call `.deleteOnExit()`, it gets added to shutdown handler which is not ideal
    * for long running systems with millions of temporary files as:
    *   a) it would slowdown shutdown and
    *   b) occupy unnecessary disk-space during app lifetime
    *
    * This util auto-deletes the resource when done using the Dispose facility
    *
    * Example usage:
    *   File.temporaryDirectory().foreach(tempDir => doSomething(tempDir)
    *
    * @return
    */
  def toTemporary: Dispose[File] =
    new Dispose(this)(Disposable.fileDisposer)

  //TODO: add features from https://github.com/sbt/io
}

object File {

  def newTemporaryDirectory(
      prefix: String = "",
      parent: Option[File] = None
  )(implicit
      attributes: Attributes = Attributes.default
  ): File = {
    parent match {
      case Some(dir) => Files.createTempDirectory(dir.path, prefix, attributes: _*)
      case _         => Files.createTempDirectory(prefix, attributes: _*)
    }
  }

  def temporaryDirectory(
      prefix: String = "",
      parent: Option[File] = None,
      attributes: Attributes = Attributes.default
  ): Dispose[File] =
    newTemporaryDirectory(prefix, parent)(attributes).toTemporary

  def usingTemporaryDirectory[U](
      prefix: String = "",
      parent: Option[File] = None,
      attributes: Attributes = Attributes.default
  )(f: File => U): Unit =
    temporaryDirectory(prefix, parent, attributes).foreach(f)

  def newTemporaryFile(
      prefix: String = "",
      suffix: String = "",
      parent: Option[File] = None
  )(implicit
      attributes: Attributes = Attributes.default
  ): File = {
    parent match {
      case Some(dir) => Files.createTempFile(dir.path, prefix, suffix, attributes: _*)
      case _         => Files.createTempFile(prefix, suffix, attributes: _*)
    }
  }

  def temporaryFile[U](
      prefix: String = "",
      suffix: String = "",
      parent: Option[File] = None,
      attributes: Attributes = Attributes.default
  ): Dispose[File] =
    newTemporaryFile(prefix, suffix, parent)(attributes).toTemporary

  def usingTemporaryFile[U](
      prefix: String = "",
      suffix: String = "",
      parent: Option[File] = None,
      attributes: Attributes = Attributes.default
  )(f: File => U): Unit =
    temporaryFile(prefix, suffix, parent, attributes).foreach(f)

  implicit def apply(path: Path): File =
    new File(path.toAbsolutePath.normalize())

  def apply(path: String, fragments: String*): File =
    Paths.get(path, fragments: _*)

  /**
    * Get File to path with help of reference anchor.
    *
    * Anchor is used as a reference in case that path is not absolute.
    * Anchor could be path to directory or path to file.
    * If anchor is file, then file's parent dir is used as an anchor.
    *
    * If anchor itself is relative, then anchor is used together with current working directory.
    *
    * NOTE: If anchor is non-existing path on filesystem, then it's always treated as file,
    * e.g. it's last component is removed when it is used as an anchor.
    *
    * @param anchor path to be used as anchor
    * @param path as string
    * @param fragments optional path fragments
    * @return absolute, normalize path
    */
  def apply(anchor: File, path: String, fragments: String*): File = {
    val p = Paths.get(path, fragments: _*)
    if (p.isAbsolute) {
      p
    } else if (anchor.isDirectory) {
      anchor / p.toString
    } else {
      anchor.parent / p.toString
    }
  }

  def apply(url: URL): File =
    apply(url.toURI)

  def apply(uri: URI): File =
    Paths.get(uri)

  def roots: Iterable[File] =
    FileSystems.getDefault.getRootDirectories.asScala.map(File.apply)

  def root: File =
    roots.head

  def home: File =
    Properties.userHome.toFile

  def temp: File =
    Properties.tmpDir.toFile

  def currentWorkingDirectory: File =
    File("")

  type Attributes = Seq[FileAttribute[_]]
  object Attributes {
    val default: Attributes = Seq.empty
  }

  type CopyOptions = Seq[CopyOption]
  object CopyOptions {
    def apply(overwrite: Boolean): CopyOptions =
      (if (overwrite) Seq(StandardCopyOption.REPLACE_EXISTING) else default) ++ LinkOptions.default
    val default: CopyOptions    = Seq.empty //Seq(StandardCopyOption.COPY_ATTRIBUTES)
    val atomically: CopyOptions = Seq(StandardCopyOption.ATOMIC_MOVE)
  }

  type Events = Seq[WatchEvent.Kind[_]]
  object Events {
    val all: Events = Seq(
      StandardWatchEventKinds.ENTRY_CREATE,
      StandardWatchEventKinds.ENTRY_MODIFY,
      StandardWatchEventKinds.ENTRY_DELETE
    )
    val default: Events = all
  }

  type OpenOptions = Seq[OpenOption]
  object OpenOptions {
    val append: OpenOptions  = Seq(StandardOpenOption.APPEND, StandardOpenOption.CREATE)
    val default: OpenOptions = Seq.empty
  }

  type LinkOptions = Seq[LinkOption]
  object LinkOptions {
    val follow: LinkOptions   = Seq.empty
    val noFollow: LinkOptions = Seq(LinkOption.NOFOLLOW_LINKS)
    val default: LinkOptions  = follow
  }

  type VisitOptions = Seq[FileVisitOption]
  object VisitOptions {
    val follow: VisitOptions  = Seq(FileVisitOption.FOLLOW_LINKS)
    val default: VisitOptions = Seq.empty
  }

  type Order = Ordering[File]
  object Order {
    val bySize: Order             = Ordering.by(_.size)
    val byName: Order             = Ordering.by(_.name)
    val byDepth: Order            = Ordering.by(_.path.getNameCount)
    val byModificationTime: Order = Ordering.by(_.lastModifiedTime)
    val byDirectoriesLast: Order  = Ordering.by(_.isDirectory)
    val byDirectoriesFirst: Order = byDirectoriesLast.reverse
    val default: Order            = byDirectoriesFirst.andThenBy(byName)
  }

  abstract class PathMatcherSyntax(name: String) {

    /**
      * Return PathMatcher from this file
      *
      * @param file
      * @param pattern
      * @param includePath If this is true, no need to include path matchers
      *                    e.g. instead of "**/ /*.txt" we can simply use *.txt
     * @return
     */
    def apply(file: File, pattern: String, includePath: Boolean): PathMatcher = {
      val escapedPath = if (includePath) escapePath(file.path.toString + file.fileSystem.getSeparator) else ""
      file.fileSystem.getPathMatcher(s"$name:$escapedPath$pattern")
    }

    def escapePath(path: String): String
  }
  object PathMatcherSyntax {
    val glob: PathMatcherSyntax = new PathMatcherSyntax("glob") {
      override def escapePath(path: String) =
        path
          .replaceAllLiterally("\\", "\\\\")
          .replaceAllLiterally("*", "\\*")
          .replaceAllLiterally("?", "\\?")
          .replaceAllLiterally("{", "\\{")
          .replaceAllLiterally("}", "\\}")
          .replaceAllLiterally("[", "\\[")
          .replaceAllLiterally("]", "\\]")
    }

    val regex: PathMatcherSyntax = new PathMatcherSyntax("regex") {
      override def escapePath(path: String) = Pattern.quote(path)
    }

    val default: PathMatcherSyntax = glob
  }

  class RandomAccessMode private (val value: String)
  object RandomAccessMode {
    val read                         = new RandomAccessMode("r")
    val readWrite                    = new RandomAccessMode("rw")
    val readWriteMetadataSynchronous = new RandomAccessMode("rws")
    val readWriteContentSynchronous  = new RandomAccessMode("rwd")
  }

  def numberOfOpenFileDescriptors(): Long = {
    java.lang.management.ManagementFactory.getPlatformMBeanServer
      .getAttribute(new javax.management.ObjectName("java.lang:type=OperatingSystem"), "OpenFileDescriptorCount")
      .asInstanceOf[Long]
  }

  /**
    * Implement this interface to monitor the root file
    */
  trait Monitor extends AutoCloseable {
    val root: File

    /**
      * Dispatch a StandardWatchEventKind to an appropriate callback
      * Override this if you don't want to manually handle onDelete/onCreate/onModify separately
      *
      * @param eventType
      * @param file
      */
    def onEvent(eventType: WatchEvent.Kind[Path], file: File, count: Int): Unit =
      eventType match {
        case StandardWatchEventKinds.ENTRY_CREATE => onCreate(file, count)
        case StandardWatchEventKinds.ENTRY_MODIFY => onModify(file, count)
        case StandardWatchEventKinds.ENTRY_DELETE => onDelete(file, count)
      }

    def start()(implicit executionContext: ExecutionContext): Unit

    def onCreate(file: File, count: Int): Unit

    def onModify(file: File, count: Int): Unit

    def onDelete(file: File, count: Int): Unit

    def onUnknownEvent(event: WatchEvent[_]): Unit

    def onException(exception: Throwable): Unit

    def stop(): Unit = close()
  }
}
