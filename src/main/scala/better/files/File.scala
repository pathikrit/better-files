package better.files

import java.io.{File => JFile, _}
import java.net.{URI, URL}
import java.nio.channels._
import java.nio.charset.Charset
import java.nio.file._
import java.nio.file.attribute._
import java.security.MessageDigest
import java.time.Instant
import java.util.function.BiPredicate
import java.util.regex.Pattern
import java.util.zip._

import scala.collection.compat._
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._
import scala.util.Properties
import scala.util.matching.Regex

/** Scala wrapper around java.nio.files.Path */
@SerialVersionUID(3435L)
class File private (val path: Path)(implicit val fileSystem: FileSystem = path.getFileSystem) extends Serializable {
  def pathAsString: String =
    path.toString

  /** getResource[...](path) always uses "/" for separator
    *   https://docs.oracle.com/javase/8/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String)
    */
  def resourcePathAsString: String =
    pathAsString.replace(JFile.separatorChar, '/')

  /** @return java.io.File version of this */
  def toJava: JFile =
    new JFile(path.toAbsolutePath.toString)

  /** Name of file
    * Certain files may not have a name e.g. root directory - returns empty string in that case
    */
  def name: String =
    nameOption.getOrElse("")

  /** Certain files may not have a name e.g. root directory - returns None in that case */
  def nameOption: Option[String] =
    Option(path.getFileName).map(_.toString)

  /** @return root of this file e.g. '/' for '/dve/null' */
  def root: File =
    path.getRoot

  /** Resolves ../../foo to the fully qualified path */
  def canonicalPath: String =
    toJava.getAbsolutePath

  /** @see canonicalFile */
  def canonicalFile: File =
    toJava.getCanonicalFile.toScala

  /** Return the file name without extension e.g. for foo.tar.gz return foo */
  def nameWithoutExtension: String =
    nameWithoutExtension(includeAll = true)

  /** @param includeAll
    *         For files with multiple extensions e.g. "bundle.tar.gz"
    *         nameWithoutExtension(includeAll = true) returns "bundle"
    *         nameWithoutExtension(includeAll = false) returns "bundle.tar"
    */
  def nameWithoutExtension(includeAll: Boolean, linkOptions: File.LinkOptions = File.LinkOptions.default): String =
    if (hasExtension(linkOptions)) name.substring(0, indexOfExtension(includeAll)) else name

  /** @param includeDot  whether the dot should be included in the extension or not
    * @param includeAll  whether all extension tokens should be included, or just the last one e.g. for bundle.tar.gz should it be .tar.gz or .gz
    * @param toLowerCase to lowercase the extension or not e.g. foo.HTML should have .html or .HTML
    * @return extension (including the dot) of this file if it is a regular file and has an extension, else None
    */
  def extension(
      includeDot: Boolean = true,
      includeAll: Boolean = false,
      toLowerCase: Boolean = true,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): Option[String] =
    when(hasExtension(linkOptions)) {
      val dot       = indexOfExtension(includeAll)
      val index     = if (includeDot) dot else dot + 1
      val extension = name.substring(index)
      if (toLowerCase) extension.toLowerCase else extension
    }

  private[this] def indexOfExtension(includeAll: Boolean): Int =
    if (includeAll) name.indexOf(".") else name.lastIndexOf(".")

  /** Returns the extension if file is a regular file
    * If file is unreadable or does not exist, it is assumed to be not a regular file
    * See: https://github.com/pathikrit/better-files/issues/89
    */
  def hasExtension(linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    (isRegularFile(linkOptions) || notExists(linkOptions)) && name.contains(".")

  /** Changes the file-extension by renaming this file; if file does not have an extension, it adds the extension
    * Example usage file"foo.java".changeExtensionTo(".scala")
    *
    * If file does not exist (or is a directory) no change is done and the current file is returned
    */
  def changeExtensionTo(extension: String, linkOptions: File.LinkOptions = File.LinkOptions.default): File = {
    val newName = s"$nameWithoutExtension.${extension.stripPrefix(".")}"
    if (isRegularFile(linkOptions)) renameTo(newName) else if (notExists(linkOptions)) File(newName) else this
  }

  /** return the content type of the file e.g. text/html */
  def contentType: Option[String] =
    Option(Files.probeContentType(path))

  /** Return parent of this file
    * NOTE: This API returns null if this file is the root;
    * please use parentOption if you expect to handle roots
    */
  def parent: File =
    parentOption.orNull

  /** @return Some(parent) of this file or None if this is the root and thus has no parent */
  def parentOption: Option[File] =
    Option(path.getParent).map(File.apply)

  /** DSL to drop to child */
  def /(child: String): File =
    path.resolve(child)

  /** DSL to drop to child */
  def /(child: Symbol): File =
    this / child.name

  def createChild(
      child: String,
      asDirectory: Boolean = false,
      createParents: Boolean = false,
      attributes: File.Attributes = File.Attributes.default,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): File =
    (this / child).createIfNotExists(asDirectory, createParents, attributes, linkOptions)

  /** Create this file. If it exists, don't do anything
    *
    * @param asDirectory   If you want this file to be created as a directory instead, set this to true (false by default)
    * @param createParents If you also want all the parents to be created from root to this file (false by default)
    */
  def createIfNotExists(
      asDirectory: Boolean = false,
      createParents: Boolean = false,
      attributes: File.Attributes = File.Attributes.default,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): this.type = {
    if (exists(linkOptions)) {
      this
    } else if (asDirectory) {
      createDirectories(attributes)
    } else {
      if (createParents) parent.createDirectories(attributes)
      try {
        createFile(attributes)
      } catch {
        case _: FileAlreadyExistsException if isRegularFile(linkOptions) => // We don't really care if it exists already
      }
      this
    }
  }

  def createFileIfNotExists(
      createParents: Boolean = false,
      attributes: File.Attributes = File.Attributes.default,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): this.type =
    createIfNotExists(asDirectory = false, createParents, attributes, linkOptions)

  def createDirectoryIfNotExists(
      createParents: Boolean = false,
      attributes: File.Attributes = File.Attributes.default,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): this.type =
    createIfNotExists(asDirectory = true, createParents, attributes, linkOptions)

  /** Create this file */
  def createFile(attributes: File.Attributes = File.Attributes.default): this.type = {
    Files.createFile(path, attributes: _*)
    this
  }

  def exists(linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    Files.exists(path, linkOptions: _*)

  def notExists(linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    Files.notExists(path, linkOptions: _*)

  def sibling(name: String): File =
    path.resolveSibling(name)

  def isSiblingOf(sibling: File): Boolean =
    sibling.isChildOf(parent)

  def siblings: Iterator[File] =
    parent.list.filterNot(_ == this)

  def isChildOf(parent: File): Boolean =
    parent.isParentOf(this)

  /** Check if this directory contains this file
    *
    * @param strict If strict is false, it would return true for self.contains(self)
    * @return true if this is a directory and it contains this file
    */
  def contains(file: File, strict: Boolean = true): Boolean =
    isDirectory() && (file.path startsWith path) && (!strict || !isSamePathAs(file))

  def isParentOf(child: File): Boolean =
    contains(child)

  def bytes: Iterator[Byte] =
    newInputStream().buffered.bytes

  /** load bytes into memory - for large files you want to use @see bytes which uses an iterator */
  def loadBytes: Array[Byte] =
    Files.readAllBytes(path)

  def byteArray: Array[Byte] =
    loadBytes

  /** Create this directory */
  def createDirectory(attributes: File.Attributes = File.Attributes.default): this.type = {
    Files.createDirectory(path, attributes: _*)
    this
  }

  /** Create this directory and all its parents
    * Unlike the JDK, this by default sanely handles the JDK-8130464 bug
    * If you want default Java behaviour, use File.LinkOptions.noFollow
    */
  def createDirectories(
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

  /** @return characters of this file */
  def chars(charset: Charset = DefaultCharset): Iterator[Char] =
    newBufferedReader(charset).chars

  /** Load all lines from this file
    * Note: Large files may cause an OutOfMemory in which case, use the streaming version @see lineIterator
    *
    * @return all lines in this file
    */
  def lines(charset: Charset = DefaultCharset): Iterable[String] =
    Files.readAllLines(path, charset).asScala

  /** @return number of lines in this file */
  def lineCount(charset: Charset = DefaultCharset): Int =
    lineIterator(charset).size

  /** Iterate over lines in a file (auto-close stream on complete)
    * If you want partial iteration use @see lines()
    */
  def lineIterator(charset: Charset = DefaultCharset): Iterator[String] =
    Files.lines(path, charset).toAutoClosedIterator

  /** Similar to string.split(), this returns tokens from the file by given splitter (default is whitespace) */
  def tokens(
      splitter: StringSplitter = StringSplitter.Default,
      charset: Charset = DefaultCharset
  ): Iterator[String] =
    newBufferedReader(charset).tokens(splitter)

  /** @return the content of this file as string */
  def contentAsString(charset: Charset = DefaultCharset): String =
    new String(byteArray, charset)

  /** Write lines into this file */
  def printLines(
      lines: IterableOnce[_],
      openOptions: File.OpenOptions = File.OpenOptions.append
  ): this.type = {
    printWriter(openOptions = openOptions).foreach(_.printLines(lines))
    this
  }

  /** For large number of lines that may not fit in memory, use printLines */
  def appendLines(lines: Seq[String], charset: Charset = DefaultCharset): this.type = {
    Files.write(path, lines.asJava, charset, File.OpenOptions.append: _*)
    this
  }

  /** If you need to specify a charset, use the above version */
  def appendLines(lines: String*): this.type =
    appendLines(lines)

  def appendLine(line: String = "", charset: Charset = DefaultCharset): this.type =
    appendLines(Seq(line), charset)

  def append(text: String, charset: Charset = DefaultCharset): this.type =
    appendByteArray(text.getBytes(charset))

  def appendText(text: String, charset: Charset = DefaultCharset): this.type =
    append(text, charset)

  def appendByteArray(bytes: Array[Byte]): this.type = {
    Files.write(path, bytes, File.OpenOptions.append: _*)
    this
  }

  def appendBytes(bytes: Iterator[Byte]): this.type =
    writeBytes(bytes, openOptions = File.OpenOptions.append)

  /** Write byte array to file. For large contents consider using the writeBytes */
  def writeByteArray(
      bytes: Array[Byte],
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): this.type = {
    Files.write(path, bytes, openOptions: _*)
    this
  }

  def writeBytes(
      bytes: Iterator[Byte],
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): this.type = {
    outputStream(openOptions).foreach(_.buffered.write(bytes))
    this
  }

  def write(
      text: String,
      openOptions: File.OpenOptions = File.OpenOptions.default,
      charset: Charset = DefaultCharset
  ): this.type =
    writeByteArray(text.getBytes(charset), openOptions)

  def writeText(
      text: String,
      openOptions: File.OpenOptions = File.OpenOptions.default,
      charset: Charset = DefaultCharset
  ): this.type =
    write(text, openOptions, charset)

  def overwrite(
      text: String,
      openOptions: File.OpenOptions = File.OpenOptions.default,
      charset: Charset = DefaultCharset
  ): this.type =
    write(text, openOptions, charset)

  def newRandomAccess(mode: File.RandomAccessMode = File.RandomAccessMode.read): RandomAccessFile =
    new RandomAccessFile(toJava, mode.value)

  def randomAccess(mode: File.RandomAccessMode = File.RandomAccessMode.read): Dispose[RandomAccessFile] =
    newRandomAccess(mode).autoClosed

  def newBufferedReader(charset: Charset = DefaultCharset): BufferedReader =
    Files.newBufferedReader(path, charset)

  def bufferedReader(charset: Charset = DefaultCharset): Dispose[BufferedReader] =
    newBufferedReader(charset).autoClosed

  def newBufferedWriter(
      charset: Charset = DefaultCharset,
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): BufferedWriter =
    Files.newBufferedWriter(path, charset, openOptions: _*)

  def bufferedWriter(
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
      autoFlush: Boolean = false,
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): PrintWriter =
    new PrintWriter(newOutputStream(openOptions), autoFlush)

  def printWriter(
      autoFlush: Boolean = false,
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): Dispose[PrintWriter] =
    newPrintWriter(autoFlush, openOptions).autoClosed

  def newInputStream(openOptions: File.OpenOptions = File.OpenOptions.default): InputStream =
    Files.newInputStream(path, openOptions: _*)

  def inputStream(openOptions: File.OpenOptions = File.OpenOptions.default): Dispose[InputStream] =
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
      splitter: StringSplitter = StringSplitter.Default,
      charset: Charset = DefaultCharset
  ): Scanner =
    Scanner(newBufferedReader(charset), splitter)

  def scanner(
      splitter: StringSplitter = StringSplitter.Default,
      charset: Charset = DefaultCharset
  ): Dispose[Scanner] =
    newScanner(splitter, charset).autoClosed

  def newOutputStream(openOptions: File.OpenOptions = File.OpenOptions.default): OutputStream =
    Files.newOutputStream(path, openOptions: _*)

  def outputStream(openOptions: File.OpenOptions = File.OpenOptions.default): Dispose[OutputStream] =
    newOutputStream(openOptions).autoClosed

  def newZipOutputStream(
      openOptions: File.OpenOptions = File.OpenOptions.default,
      charset: Charset = DefaultCharset
  ): ZipOutputStream =
    new ZipOutputStream(newOutputStream(openOptions), charset)

  def zipInputStream(charset: Charset = DefaultCharset): Dispose[ZipInputStream] =
    newZipInputStream(charset).autoClosed

  def newZipInputStream(charset: Charset = DefaultCharset): ZipInputStream =
    new ZipInputStream(newFileInputStream.buffered, charset)

  def zipOutputStream(
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

  def newFileChannel(
      openOptions: File.OpenOptions = File.OpenOptions.default,
      attributes: File.Attributes = File.Attributes.default
  ): FileChannel =
    FileChannel.open(path, openOptions.toSet.asJava, attributes: _*)

  def fileChannel(
      openOptions: File.OpenOptions = File.OpenOptions.default,
      attributes: File.Attributes = File.Attributes.default
  ): Dispose[FileChannel] =
    newFileChannel(openOptions, attributes).autoClosed

  def newAsynchronousFileChannel(
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): AsynchronousFileChannel =
    AsynchronousFileChannel.open(path, openOptions: _*)

  def asynchronousFileChannel(
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): Dispose[AsynchronousFileChannel] =
    newAsynchronousFileChannel(openOptions).autoClosed

  def newWatchService: WatchService =
    fileSystem.newWatchService()

  def watchService: Dispose[WatchService] =
    newWatchService.autoClosed

  /** Serialize a object using Java's serializer into this file, creating it and its parents if they do not exist */
  def writeSerialized(
      obj: Serializable,
      bufferSize: Int = DefaultBufferSize,
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): this.type = {
    createFileIfNotExists(createParents = true)
      .outputStream(openOptions)
      .foreach(_.asObjectOutputStream(bufferSize).serialize(obj).flush())
    this
  }

  /** Deserialize a object using Java's default serialization from this file */
  def readDeserialized[A](
      classLoaderOverride: Option[ClassLoader] = None,
      bufferSize: Int = DefaultBufferSize,
      openOptions: File.OpenOptions = File.OpenOptions.default
  ): A =
    classLoaderOverride match {
      case Some(classLoader) =>
        inputStream(openOptions).apply(_.asObjectInputStreamUsingClassLoader(classLoader, bufferSize).deserialize[A])
      case _ => inputStream(openOptions).apply(_.asObjectInputStream(bufferSize).deserialize[A])
    }

  /** register a watch service for given events */
  def register(service: WatchService, events: File.Events = File.Events.all): this.type = {
    path.register(service, events.toArray)
    this
  }

  def digest(
      algorithm: MessageDigest,
      visitOptions: File.VisitOptions = File.VisitOptions.default,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): Array[Byte] = {
    listRelativePaths(visitOptions).toSeq.sorted foreach { relativePath =>
      val file: File = path.resolve(relativePath)
      if (file.isDirectory(linkOptions)) {
        algorithm.update(relativePath.toString.getBytes)
      } else {
        file.newInputStream().withMessageDigest(algorithm).autoClosed.foreach(_.pipeTo(NullOutputStream))
      }
    }
    algorithm.digest()
  }

  /** Set a file attribute e.g. file("dos:system") = true */
  def update(
      attribute: String,
      value: Any,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): this.type = {
    Files.setAttribute(path, attribute, value, linkOptions: _*)
    this
  }

  /** @return checksum of this file (or directory) in hex format */
  def checksum(
      algorithm: MessageDigest,
      visitOptions: File.VisitOptions = File.VisitOptions.default,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): String =
    toHex(digest(algorithm, visitOptions, linkOptions))

  def md5(visitOptions: File.VisitOptions = File.VisitOptions.default, linkOptions: File.LinkOptions = File.LinkOptions.default): String =
    checksum("MD5", visitOptions, linkOptions)

  def sha1(visitOptions: File.VisitOptions = File.VisitOptions.default, linkOptions: File.LinkOptions = File.LinkOptions.default): String =
    checksum("SHA-1", visitOptions, linkOptions)

  def sha256(
      visitOptions: File.VisitOptions = File.VisitOptions.default,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): String =
    checksum("SHA-256", visitOptions, linkOptions)

  def sha512(
      visitOptions: File.VisitOptions = File.VisitOptions.default,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): String =
    checksum("SHA-512", visitOptions, linkOptions)

  /** @return Some(target) if this is a symbolic link (to target) else None */
  def symbolicLink: Option[File] =
    when(isSymbolicLink)(new File(Files.readSymbolicLink(path)))

  /** @return true if this file (or the file found by following symlink) is a directory */
  def isDirectory(linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    Files.isDirectory(path, linkOptions: _*)

  /** @return true if this file (or the file found by following symlink) is a regular file */
  def isRegularFile(linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    Files.isRegularFile(path, linkOptions: _*)

  def isSymbolicLink: Boolean =
    Files.isSymbolicLink(path)

  def isHidden: Boolean =
    Files.isHidden(path)

  /** List files recursively up to given depth using a custom file filter */
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

  /** Check if a file is locked.
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
      isShared: Boolean = false,
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

  /** @see https://docs.oracle.com/javase/tutorial/essential/io/check.html
    * @see https://stackoverflow.com/questions/30520179/why-does-file-exists-return-true-even-though-files-exists-in-the-nio-files
    *
    * @return
    *         Some(true) if file is guaranteed to exist
    *         Some(false) if file is guaranteed to not exist
    *         None if the status is unknown e.g. if file is unreadable
    */
  def verifiedExists(linkOptions: File.LinkOptions = File.LinkOptions.default): Option[Boolean] = {
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

  def listRecursively(visitOptions: File.VisitOptions = File.VisitOptions.default): Iterator[File] =
    walk(visitOptions = visitOptions).filterNot(isSamePathAs)

  /** Walk the directory tree recursively upto maxDepth
    * @return List of children in BFS maxDepth level deep (includes self since self is at depth = 0)
    */
  def walk(
      maxDepth: Int = Int.MaxValue,
      visitOptions: File.VisitOptions = File.VisitOptions.default
  ): Iterator[File] =
    Files.walk(path, maxDepth, visitOptions: _*)

  def pathMatcher(syntax: File.PathMatcherSyntax, includePath: Boolean)(pattern: String): PathMatcher =
    syntax(this, pattern, includePath)

  /** Util to glob from this file's path
    *
    * @param includePath If true, we don't need to set path glob patterns
    *                    e.g. instead of **.txt we just use *.txt
   * @param maxDepth Recurse up to maxDepth
   * @return Set of files that matched
   */
  def glob(
      pattern: String,
      includePath: Boolean = true,
      maxDepth: Int = Int.MaxValue,
      syntax: File.PathMatcherSyntax = File.PathMatcherSyntax.default,
      visitOptions: File.VisitOptions = File.VisitOptions.default
  ): Iterator[File] =
    pathMatcher(syntax, includePath)(pattern).matches(this, maxDepth, visitOptions)

  /** Util to match from this file's path using Regex
    *
    * @param includePath If true, we don't need to set path glob patterns
    *                    e.g. instead of **.txt we just use *.txt
   * @param maxDepth Recurse up to maxDepth
   * @see glob
   * @return Set of files that matched
   */
  def globRegex(
      pattern: Regex,
      includePath: Boolean = true,
      maxDepth: Int = Int.MaxValue,
      visitOptions: File.VisitOptions = File.VisitOptions.default
  ): Iterator[File] =
    glob(pattern.regex, includePath, maxDepth, File.PathMatcherSyntax.regex, visitOptions)

  /** More Scala friendly way of doing Files.walk
    * Note: This is lazy (returns an Iterator) and won't evaluate till we reify the iterator (e.g. using .toList)
    */
  def collectChildren(
      matchFilter: File => Boolean,
      maxDepth: Int = Int.MaxValue,
      visitOptions: File.VisitOptions = File.VisitOptions.default
  ): Iterator[File] =
    walk(maxDepth, visitOptions).filter(matchFilter)

  def uri: URI =
    path.toUri

  def url: URL =
    uri.toURL

  /** @param returnZeroIfMissing If true, return zeroes for missing files*
    * @return file size (for directories, return size of the directory) in bytes
    */
  def size(returnZeroIfMissing: Boolean = isDirectory(), visitOptions: File.VisitOptions = File.VisitOptions.default): Long =
    walk(visitOptions = visitOptions)
      .map({ f =>
        try {
          Files.size(f.path)
        } catch {
          case (_: FileNotFoundException | _: NoSuchFileException) if returnZeroIfMissing => 0L
        }
      })
      .sum

  def permissions(linkOptions: File.LinkOptions = File.LinkOptions.default): Set[PosixFilePermission] =
    Files.getPosixFilePermissions(path, linkOptions: _*).asScala.toSet

  def permissionsAsString(linkOptions: File.LinkOptions = File.LinkOptions.default): String =
    PosixFilePermissions.toString(permissions(linkOptions).asJava)

  def setPermissions(permissions: Set[PosixFilePermission]): this.type = {
    Files.setPosixFilePermissions(path, permissions.asJava)
    this
  }

  def addPermission(
      permission: PosixFilePermission,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): this.type =
    setPermissions(permissions(linkOptions) + permission)

  def removePermission(
      permission: PosixFilePermission,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): this.type =
    setPermissions(permissions(linkOptions) - permission)

  /** test if file has this permission */
  def testPermission(
      permission: PosixFilePermission,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): Boolean =
    permissions(linkOptions)(permission)

  def isOwnerReadable(linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.OWNER_READ, linkOptions)

  def isOwnerWritable(linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.OWNER_WRITE, linkOptions)

  def isOwnerExecutable(linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.OWNER_EXECUTE, linkOptions)

  def isGroupReadable(linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.GROUP_READ, linkOptions)

  def isGroupWritable(linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.GROUP_WRITE, linkOptions)

  def isGroupExecutable(linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.GROUP_EXECUTE, linkOptions)

  def isOthersReadable(linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.OTHERS_READ, linkOptions)

  def isOthersWritable(linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.OTHERS_WRITE, linkOptions)

  def isOthersExecutable(linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    testPermission(PosixFilePermission.OTHERS_EXECUTE, linkOptions)

  /** This differs from the above as this checks if the JVM can read this file even though the OS cannot in certain platforms */
  def isReadable: Boolean =
    toJava.canRead

  def isWritable: Boolean =
    toJava.canWrite

  def isExecutable: Boolean =
    toJava.canExecute

  def attributes(linkOptions: File.LinkOptions = File.LinkOptions.default): BasicFileAttributes =
    Files.readAttributes(path, classOf[BasicFileAttributes], linkOptions: _*)

  def posixAttributes(linkOptions: File.LinkOptions = File.LinkOptions.default): PosixFileAttributes =
    Files.readAttributes(path, classOf[PosixFileAttributes], linkOptions: _*)

  def dosAttributes(linkOptions: File.LinkOptions = File.LinkOptions.default): DosFileAttributes =
    Files.readAttributes(path, classOf[DosFileAttributes], linkOptions: _*)

  def owner(linkOptions: File.LinkOptions = File.LinkOptions.default): UserPrincipal =
    Files.getOwner(path, linkOptions: _*)

  def ownerName(linkOptions: File.LinkOptions = File.LinkOptions.default): String =
    owner(linkOptions).getName

  def group(linkOptions: File.LinkOptions = File.LinkOptions.default): GroupPrincipal =
    posixAttributes(linkOptions).group()

  def groupName(linkOptions: File.LinkOptions = File.LinkOptions.default): String =
    group(linkOptions).getName

  def setOwner(owner: String): this.type = {
    Files.setOwner(path, fileSystem.getUserPrincipalLookupService.lookupPrincipalByName(owner))
    this
  }

  def setGroup(group: String, linkOptions: File.LinkOptions = File.LinkOptions.default): this.type = {
    Files
      .getFileAttributeView(path, classOf[PosixFileAttributeView], linkOptions: _*)
      .setGroup(fileSystem.getUserPrincipalLookupService.lookupPrincipalByGroupName(group))
    this
  }

  /** Similar to the UNIX command touch - create this file if it does not exist and set its last modification time */
  def touch(
      time: Instant = Instant.now(),
      attributes: File.Attributes = File.Attributes.default,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): this.type = {
    Files.setLastModifiedTime(createFileIfNotExists(attributes = attributes, linkOptions = linkOptions).path, FileTime.from(time))
    this
  }

  def lastModifiedTime(linkOptions: File.LinkOptions = File.LinkOptions.default): Instant =
    Files.getLastModifiedTime(path, linkOptions: _*).toInstant

  /** Deletes this file or directory
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
      case _: IOException if swallowIOExceptions => // e.printStackTrace() //swallow
    }
    this
  }

  def renameTo(newName: String): File =
    moveTo(path.resolveSibling(newName))

  /** Move this to destination similar to unix mv command
    * @see moveToDirectory() if you want to move _into_ another directory
    */
  def moveTo(
      destination: File,
      copyOptions: File.CopyOptions = File.CopyOptions(overwrite = false)
  ): destination.type = {
    Files.move(path, destination.path, copyOptions: _*)
    destination
  }

  /** Moves this file into the given directory
    * @return the File referencing the new file created under destination
    */
  def moveToDirectory(directory: File, linkOptions: File.LinkOptions = File.LinkOptions.default): File = {
    require(directory.isDirectory(linkOptions), s"$directory must be a directory")
    moveTo(directory / this.name)
  }

  /** Copy this file to destination similar to unix cp command
    *
    * @see copyToDirectory() if you want to copy _into_ another directory
    */
  def copyTo(destination: File, overwrite: Boolean = false): destination.type =
    copyTo(destination, File.CopyOptions(overwrite))

  def copyTo(destination: File, copyOptions: File.CopyOptions): destination.type = {
    if (isDirectory()) {
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

  /** Copies this file into the given directory
    * @return the File referencing the new file created under destination
    */
  def copyToDirectory(
      directory: File,
      overwrite: Boolean = false,
      linkOptions: File.LinkOptions = File.LinkOptions.default
  ): File = {
    require(directory.isDirectory(linkOptions), s"$directory must be a directory")
    copyTo(directory / this.name, overwrite)
  }

  def symbolicLinkTo(
      destination: File,
      attributes: File.Attributes = File.Attributes.default
  ): destination.type = {
    Files.createSymbolicLink(path, destination.path, attributes: _*)
    destination
  }

  def linkTo(
      destination: File,
      symbolic: Boolean = false,
      attributes: File.Attributes = File.Attributes.default
  ): destination.type = {
    if (symbolic) {
      symbolicLinkTo(destination, attributes)
    } else {
      Files.createLink(destination.path, path)
      destination
    }
  }

  def listRelativePaths(visitOptions: File.VisitOptions = File.VisitOptions.default): Iterator[Path] =
    walk(visitOptions = visitOptions).map(relativize)

  def relativize(destination: File): Path =
    path.relativize(destination.path)

  def isSamePathAs(that: File): Boolean =
    this.path == that.path

  def isSameFileAs(that: File): Boolean =
    Files.isSameFile(this.path, that.path)

  /** @return true if this file is exactly same as that file
    *         For directories, it checks for equivalent directory structure
    */
  def isSameContentAs(that: File): Boolean =
    isSimilarContentAs(that)

  /** Almost same as isSameContentAs but uses faster md5 hashing to compare (and thus small chance of false positive)
    * Also works for directories
    */
  def isSimilarContentAs(that: File): Boolean =
    this.md5() == that.md5()

  override def equals(obj: Any) = {
    obj match {
      case file: File => isSamePathAs(file)
      case _          => false
    }
  }

  /** @return true if file is not present or empty directory or 0-bytes file */
  def isEmpty(linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean = {
    if (isDirectory(linkOptions)) {
      Files.list(path).autoClosed(_.count()) == 0 // Do not use children.isEmpty as it may leave stream open
    } else if (isRegularFile(linkOptions)) {
      toJava.length() == 0
    } else {
      notExists(linkOptions)
    }
  }

  /** @return for directories, true if it has no children, false otherwise
    *         for files, true if it is a 0-byte file, false otherwise
    *         else true if it exists, false otherwise
    */
  def nonEmpty(linkOptions: File.LinkOptions = File.LinkOptions.default): Boolean =
    !isEmpty(linkOptions)

  /** If this is a directory, remove all its children
    * If its a file, empty the contents
    */
  def clear(linkOptions: File.LinkOptions = File.LinkOptions.default): this.type = {
    if (isDirectory(linkOptions)) {
      children.foreach(_.delete())
    } else {
      writeByteArray(Array.emptyByteArray)
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
      case _: IOException if swallowIOExceptions => // e.printStackTrace() //swallow
    }
    this
  }

  override def hashCode =
    path.hashCode()

  override def toString =
    pathAsString

  /** Zips this file (or directory)
    *
    * @param destination The destination file; Creates this if it does not exists
    * @return The destination zip file
    */
  def zipTo(
      destination: File,
      compressionLevel: Int = Deflater.DEFAULT_COMPRESSION,
      charset: Charset = DefaultCharset
  ): destination.type = {
    val files = if (isDirectory()) children else Iterator(this)
    destination.zipIn(files, compressionLevel, charset)
  }

  /** zip to a temp directory
    *
    * @return the target directory
    */
  def zip(compressionLevel: Int = Deflater.DEFAULT_COMPRESSION, charset: Charset = DefaultCharset): File =
    zipTo(destination = File.newTemporaryFile(prefix = name, suffix = ".zip"), compressionLevel, charset)

  /** Unzips this zip file
    *
    * @param destination destination folder; Creates this if it does not exist
    * @param zipFilter An optional param to reject or accept unzipping a file
    * @param charset Optional charset parameter
    * @param safeUnzip Optional parameter to set whether unzipTo should unzip entries containing directory traversal characters to directories outside the destination
    * @return The destination where contents are unzipped
    */
  def unzipTo(
      destination: File = File.newTemporaryDirectory(name.stripSuffix(".zip")),
      zipFilter: ZipEntry => Boolean = _ => true,
      charset: Charset = DefaultCharset,
      safeUnzip: Boolean = true
  ): destination.type = {
    for {
      zipFile <- new ZipFile(toJava, charset).autoClosed
      entry   <- zipFile.entries().asScala if zipFilter(entry)
    } if (safeUnzip && File(destination, entry.getName).canonicalPath.startsWith(destination.canonicalPath + JFile.separator)) {
      // https://developer.android.com/topic/security/risks/zip-path-traversal
      entry.extractTo(destination, zipFile.getInputStream(entry))
    } else if (!safeUnzip) {
      entry.extractTo(destination, zipFile.getInputStream(entry))
    }
    destination
  }

  /** Streamed unzipping is slightly slower but supports larger files and more encodings
    * @see https://github.com/pathikrit/better-files/issues/152
    *
    * @param destinationDirectory destination folder; Creates this if it does not exist
    * @return The destination where contents are unzipped
    */
  def streamedUnzip(
      destinationDirectory: File = File.newTemporaryDirectory(name.stripSuffix(".zip")),
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

  /** Adds these files into this zip file
    * Example usage: File("test.zip").zipIn(Seq(file"hello.txt", file"hello2.txt"))
    */
  def zipIn(
      files: Iterator[File],
      compressionLevel: Int = Deflater.DEFAULT_COMPRESSION,
      charset: Charset = DefaultCharset
  ): this.type = {
    for {
      output <- newZipOutputStream(File.OpenOptions.default, charset).withCompressionLevel(compressionLevel).autoClosed
      input  <- files
      file   <- input.walk() if !name.contentEquals(file.name) // See https://github.com/pathikrit/better-files/pull/436
      name = input.parent.relativize(file)
    } output.add(file, name.toString)
    this
  }

  /** unzip to a temporary zip file
    *
    * @return the zip file
    */
  def unzip(zipFilter: ZipEntry => Boolean = _ => true, charset: Charset = DefaultCharset): File =
    unzipTo(destination = File.newTemporaryDirectory(name.stripSuffix(".zip")), zipFilter, charset)

  /** Java's temporary files/directories are not cleaned up by default.
    * If we explicitly call `.deleteOnExit()`, it gets added to shutdown handler which is not ideal
    * for long running systems with millions of temporary files as:
    *   a) it would slowdown shutdown and
    *   b) occupy unnecessary disk-space during app lifetime
    *
    * This util auto-deletes the resource when done using the Dispose facility
    *
    * Example usage:
    *   File.temporaryDirectory().foreach(tempDir => doSomething(tempDir)
    */
  def toTemporary: Dispose[File] =
    new Dispose(this)(Disposable.fileDisposer)
}

object File {

  def newTemporaryDirectory(
      prefix: String = "",
      parent: Option[File] = None,
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
    newTemporaryDirectory(prefix, parent, attributes).toTemporary

  def usingTemporaryDirectory[U](
      prefix: String = "",
      parent: Option[File] = None,
      attributes: Attributes = Attributes.default
  )(f: File => U): Unit =
    temporaryDirectory(prefix, parent, attributes).foreach(f)

  def newTemporaryFile(
      prefix: String = "",
      suffix: String = "",
      parent: Option[File] = None,
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
    newTemporaryFile(prefix, suffix, parent, attributes).toTemporary

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

  /** Get File to path with help of reference anchor.
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
    } else if (anchor.isDirectory()) {
      anchor / p.toString
    } else {
      anchor.parent / p.toString
    }
  }

  def apply(url: URL): File =
    apply(url.toURI)

  def apply(uri: URI): File = {
    require(uri.isFile, s"Not a file: $uri")
    Paths.get(uri)
  }

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
    val default: CopyOptions    = Seq.empty // Seq(StandardCopyOption.COPY_ATTRIBUTES)
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
    val bySize: Order             = Ordering.by(_.size())
    val byName: Order             = Ordering.by(_.name)
    val byDepth: Order            = Ordering.by(_.path.getNameCount)
    val byModificationTime: Order = Ordering.by(_.lastModifiedTime())
    val byDirectoriesLast: Order  = Ordering.by(_.isDirectory())
    val byDirectoriesFirst: Order = byDirectoriesLast.reverse
    val default: Order            = byDirectoriesFirst.andThenBy(byName)
  }

  abstract class PathMatcherSyntax(name: String) {

    /** Return PathMatcher from this file
      *
      * @param file
      * @param pattern
      * @param includePath If this is true, no need to include path matchers
      *                    e.g. instead of "**.txt" we can simply use *.txt
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
          .replace("\\", "\\\\")
          .replace("*", "\\*")
          .replace("?", "\\?")
          .replace("{", "\\{")
          .replace("}", "\\}")
          .replace("[", "\\[")
          .replace("]", "\\]")
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

  /** Implement this interface to monitor the root file */
  trait Monitor extends AutoCloseable {
    val root: File

    /** Dispatch a StandardWatchEventKind to an appropriate callback
      * Override this if you don't want to manually handle onDelete/onCreate/onModify separately
      */
    def onEvent(eventType: WatchEvent.Kind[Path], file: File, count: Int): Unit =
      eventType match {
        case StandardWatchEventKinds.ENTRY_CREATE => onCreate(file, count)
        case StandardWatchEventKinds.ENTRY_MODIFY => onModify(file, count)
        case StandardWatchEventKinds.ENTRY_DELETE => onDelete(file, count)
        case _                                    => onUnknownEvent(null)
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
