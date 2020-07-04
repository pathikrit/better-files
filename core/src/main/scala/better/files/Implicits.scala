package better.files

import java.io.{File => JFile, _}
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.{Path, PathMatcher}
import java.security.{DigestInputStream, DigestOutputStream, MessageDigest}
import java.util.StringTokenizer
import java.util.stream.{Stream => JStream}
import java.util.zip._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import java.net.URI
import java.net.URL

object Implicits {
  //TODO: Rename all Ops to Extensions

  final class StringInterpolations(val sc: StringContext) extends AnyVal {
    def file(args: Any*): File =
      value(args).toFile

    private[this] def value(args: Seq[Any]) =
      sc.s(args: _*)
  }

  final class StringExtensions(val str: String) extends AnyVal {
    def toFile: File =
      File(str)

    def /(child: String): File =
      toFile / child

    def inputStream(implicit charset: Charset = DefaultCharset): InputStream =
      new ByteArrayInputStream(str.getBytes(charset))

    def reader: Reader =
      new StringReader(str)
  }

  final class FileExtensions(val file: JFile) extends AnyVal {
    def toScala: File =
      File(file.getPath)
  }

  final class URLExtensions(val url: URL) extends AnyVal {
    def isFile: Boolean = {
      if (null == url) return false
      val uri: URI = url.toURI
      uri.getScheme == "file"
    }

    def toFile: File = {
      require(isFile, s"Not a file: $url")
      File(url)
    }

    def toFileOption: Option[File] = if (isFile) Some(File(url)) else None
  }

  final class URIExtensions(val uri: URI) extends AnyVal {
    def isFile: Boolean =
      if (null == uri) false
      else uri.getScheme() == "file"

    def toFile: File = {
      require(isFile, s"Not a file: $uri")
      File(uri)
    }

    def toFileOption: Option[File] = if (isFile) Some(File(uri)) else None
  }

  final class SymbolExtensions(val symbol: Symbol) extends AnyVal {
    def /(child: Symbol): File =
      File(symbol.name) / child
  }

  final class IteratorExtensions[A](val it: Iterator[A]) extends AnyVal {
    def withHasNext(f: => Boolean): Iterator[A] =
      new Iterator[A] {
        override def hasNext = f && it.hasNext
        override def next()  = it.next()
      }
  }

  final class InputStreamExtensions(val in: InputStream) extends AnyVal {
    def pipeTo(out: OutputStream, bufferSize: Int = DefaultBufferSize): out.type =
      pipeTo(out, Array.ofDim[Byte](bufferSize))

    /**
      * Pipe an input stream to an output stream using a byte buffer
      */
    @tailrec final def pipeTo(out: OutputStream, buffer: Array[Byte]): out.type = {
      val n = in.read(buffer)
      if (n > 0) {
        out.write(buffer, 0, n)
        pipeTo(out, buffer)
      } else {
        out
      }
    }

    def asString(
        closeStream: Boolean = true,
        bufferSize: Int = DefaultBufferSize
    )(implicit
        charset: Charset = DefaultCharset
    ): String = {
      try {
        new ByteArrayOutputStream(bufferSize).autoClosed
          .apply(pipeTo(_, bufferSize = bufferSize).toString(charset.displayName()))
      } finally {
        if (closeStream) in.close()
      }
    }

    def withMessageDigest(digest: MessageDigest): DigestInputStream =
      new DigestInputStream(in, digest)

    def md5: DigestInputStream =
      withMessageDigest("MD5")

    def sha1: DigestInputStream =
      withMessageDigest("SHA-1")

    def sha256: DigestInputStream =
      withMessageDigest("SHA-256")

    def sha512: DigestInputStream =
      withMessageDigest("SHA-512")

    def crc32: CheckedInputStream =
      withChecksum(new CRC32)

    def adler32: CheckedInputStream =
      withChecksum(new Adler32)

    def withChecksum(checksum: Checksum): CheckedInputStream =
      new CheckedInputStream(in, checksum)

    def buffered: BufferedInputStream =
      new BufferedInputStream(in)

    def buffered(bufferSize: Int): BufferedInputStream =
      new BufferedInputStream(in, bufferSize)

    def asGzipInputStream(bufferSize: Int = DefaultBufferSize): GZIPInputStream =
      new GZIPInputStream(in, bufferSize)

    def asZipInputStream(implicit charset: Charset = DefaultCharset): ZipInputStream =
      new ZipInputStream(in, charset)

    /**
      * If bufferSize is set to less than or equal to 0, we don't buffer
      * @param bufferSize
      * @return
      */
    def asObjectInputStream(bufferSize: Int = DefaultBufferSize): ObjectInputStream =
      new ObjectInputStream(if (bufferSize <= 0) in else buffered(bufferSize))

    /**
      * @param bufferSize If bufferSize is set to less than or equal to 0, we don't buffer
      * Code adapted from:
      * https://github.com/apache/commons-io/blob/master/src/main/java/org/apache/commons/io/input/ClassLoaderObjectInputStream.java
      *
      * @return A special ObjectInputStream that loads a class based on a specified ClassLoader rather than the default
      * This is useful in dynamic container environments.
      */
    def asObjectInputStreamUsingClassLoader(
        classLoader: ClassLoader = getClass.getClassLoader,
        bufferSize: Int = DefaultBufferSize
    ): ObjectInputStream =
      new ObjectInputStream(if (bufferSize <= 0) in else buffered(bufferSize)) {
        override protected def resolveClass(objectStreamClass: ObjectStreamClass): Class[_] =
          try {
            Class.forName(objectStreamClass.getName, false, classLoader)
          } catch {
            case _: ClassNotFoundException ⇒ super.resolveClass(objectStreamClass)
          }
      }

    def reader(implicit charset: Charset = DefaultCharset): InputStreamReader =
      new InputStreamReader(in, charset)

    def lines(implicit charset: Charset = DefaultCharset): Iterator[String] =
      reader(charset).buffered.lines().toAutoClosedIterator

    def bytes: Iterator[Byte] =
      in.autoClosed.flatMap(res => eofReader(res.read()).map(_.toByte))

    def byteArray: Array[Byte] = {
      for {
        _   <- in.autoClosed
        out <- new ByteArrayOutputStream().autoClosed
      } yield pipeTo(out).toByteArray
    }.get()
  }

  final class DigestInputStreamExtensions(val in: DigestInputStream) extends AnyVal {

    /** Exhausts the stream and computes the digest and closes the stream */
    def digest(drainTo: OutputStream = NullOutputStream): Array[Byte] = {
      in.autoClosed.foreach(_.pipeTo(drainTo))
      in.getMessageDigest.digest()
    }

    /** Exhausts the stream and computes the digest as hex and closes the stream */
    def hexDigest(drainTo: OutputStream = NullOutputStream): String =
      toHex(digest(drainTo))
  }

  final class OutputStreamExtensions(val out: OutputStream) extends AnyVal {
    def buffered: BufferedOutputStream =
      new BufferedOutputStream(out)

    def buffered(bufferSize: Int): BufferedOutputStream =
      new BufferedOutputStream(out, bufferSize)

    def asGzipOutputStream(bufferSize: Int = DefaultBufferSize, syncFlush: Boolean = false): GZIPOutputStream =
      new GZIPOutputStream(out, bufferSize, syncFlush)

    def withMessageDigest(digest: MessageDigest): DigestOutputStream =
      new DigestOutputStream(out, digest)

    def withChecksum(checksum: Checksum): CheckedOutputStream =
      new CheckedOutputStream(out, checksum)

    def writer(implicit charset: Charset = DefaultCharset): OutputStreamWriter =
      new OutputStreamWriter(out, charset)

    def printWriter(autoFlush: Boolean = false): PrintWriter =
      new PrintWriter(out, autoFlush)

    def write(bytes: Iterator[Byte], bufferSize: Int = DefaultBufferSize): out.type = {
      bytes.grouped(bufferSize).foreach(buffer => out.write(buffer.toArray))
      out.flush()
      out
    }

    def md5: DigestOutputStream =
      withMessageDigest("MD5")

    def sha1: DigestOutputStream =
      withMessageDigest("SHA-1")

    def sha256: DigestOutputStream =
      withMessageDigest("SHA-256")

    def sha512: DigestOutputStream =
      withMessageDigest("SHA-512")

    def crc32: CheckedOutputStream =
      withChecksum(new CRC32)

    def adler32: CheckedOutputStream =
      withChecksum(new Adler32)

    def writeAndClose(str: String)(implicit charset: Charset = DefaultCharset): Unit =
      out.writer.autoClosed.foreach(_.write(str))

    def tee(out2: OutputStream): OutputStream =
      new TeeOutputStream(out, out2)

    /**
      * If bufferSize is set to less than or equal to 0, we don't buffer
      * @param bufferSize
      * @return
      */
    def asObjectOutputStream(bufferSize: Int = DefaultBufferSize): ObjectOutputStream =
      new ObjectOutputStream(if (bufferSize <= 0) out else buffered(bufferSize))

    def asZipOutputStream(implicit charset: Charset): ZipOutputStream =
      new ZipOutputStream(out, charset)
  }

  final class PrintWriterExtensions(val pw: PrintWriter) extends AnyVal {
    def printLines(lines: TraversableOnce[_]): PrintWriter = {
      lines.foreach(pw.println)
      pw
    }
  }

  final class ReaderExtensions(val reader: Reader) extends AnyVal {
    def buffered: BufferedReader =
      new BufferedReader(reader)

    def toInputStream(implicit charset: Charset = DefaultCharset): InputStream =
      new ReaderInputStream(reader)(charset)

    def chars: Iterator[Char] =
      new Dispose(reader).flatMap(res => eofReader(res.read()).map(_.toChar))
  }

  final class BufferedReaderExtensions(val reader: BufferedReader) extends AnyVal {
    def tokens(splitter: StringSplitter = StringSplitter.Default): Iterator[String] =
      reader.lines().toAutoClosedIterator.flatMap(splitter.split)
  }

  final class WriterExtensions(val writer: Writer) extends AnyVal {
    def buffered: BufferedWriter =
      new BufferedWriter(writer)

    def outputstream(implicit charset: Charset = DefaultCharset): OutputStream =
      new WriterOutputStream(writer)(charset)
  }

  final class FileChannelExtensions(val fc: FileChannel) extends AnyVal {
    def toMappedByteBuffer: MappedByteBuffer =
      fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())
  }

  final class PathMatcherExtensions(val matcher: PathMatcher) extends AnyVal {
    def matches(file: File, maxDepth: Int)(implicit visitOptions: File.VisitOptions = File.VisitOptions.default) =
      file.collectChildren(child => matcher.matches(child.path), maxDepth)(visitOptions)
  }

  final class ObjectInputStreamExtensions(val ois: ObjectInputStream) extends AnyVal {
    def deserialize[A]: A =
      ois.readObject().asInstanceOf[A]
  }

  final class ObjectOutputStreamExtensions(val oos: ObjectOutputStream) extends AnyVal {
    def serialize(obj: Serializable): oos.type = {
      oos.writeObject(obj)
      oos
    }
  }

  final class ZipOutputStreamExtensions(val out: ZipOutputStream) extends AnyVal {

    /**
      * Correctly set the compression level
      * See: http://stackoverflow.com/questions/1206970/creating-zip-using-zip-utility
      *
      * @param level
      * @return
      */
    def withCompressionLevel(level: Int): out.type = {
      out.setLevel(level)
      if (level == Deflater.NO_COMPRESSION) out.setMethod(ZipOutputStream.DEFLATED)
      out
    }

    def add(file: File, name: String): out.type = {
      val relativeName = name.stripSuffix(file.fileSystem.getSeparator)
      val entryName =
        if (file.isDirectory) s"$relativeName/" else relativeName // make sure to end directories in ZipEntry with "/"
      out.putNextEntry(new ZipEntry(entryName))
      if (file.isRegularFile) file.inputStream.foreach(_.pipeTo(out))
      out.closeEntry()
      out
    }

    def +=(file: File): out.type =
      add(file, file.name)
  }

  final class ZipInputStreamExtensions(val in: ZipInputStream) extends AnyVal {

    /**
      * Apply `f` on each ZipEntry in the archive, closing the entry after `f` has been applied.
      *
      * @param f The function to apply to each ZipEntry. Can fail if it returns a lazy value,
      *          like Iterator, as the entry will have been closed before the lazy value is evaluated.
      */
    def mapEntries[A](f: ZipEntry => A): Iterator[A] =
      new Iterator[A] {
        private[this] var entry = in.getNextEntry

        override def hasNext = entry != null

        override def next() = {
          try {
            f(entry)
          } finally {
            try {
              in.closeEntry()
            } finally {
              entry = in.getNextEntry
            }
          }
        }
      }

    /**
      * Apply `f` to the ZipInputStream for every entry in the archive.
      * @param f The function to apply to the ZipInputStream. Can fail if it returns a lazy value,
      *          like Iterator, as the the entry will have been closed before the lazy value is evaluated.
      */
    def foldMap[A](f: ZipInputStream => A): Iterator[A] =
      mapEntries(_ => f(in))
  }

  final class ZipEntryExtensions(val entry: ZipEntry) extends AnyVal {

    /**
      * Extract this ZipEntry under this rootDir
      *
      * @param rootDir directory under which this entry is extracted
      * @param inputStream use this inputStream when this entry is a file
      * @return the extracted file
      */
    def extractTo(rootDir: File, inputStream: => InputStream): File = {
      val entryName = entry.getName.replace("\\", "/") //see https://github.com/pathikrit/better-files/issues/262
      val child     = rootDir.createChild(entryName, asDirectory = entry.isDirectory, createParents = true)
      if (!entry.isDirectory) child.outputStream.foreach(inputStream.pipeTo(_))
      child
    }
  }

  final class DisposeableExtensions[A: Disposable](val resource: A) {

    /**
      * Lightweight automatic resource management
      * Closes the resource when done e.g.
      * <pre>
      * for {
      * in <- file.newInputStream.autoClosed
      * } in.write(bytes)
      * // in is closed now
      * </pre>
      *
      * @return
      */
    def autoClosed: Dispose[A] =
      new Dispose(resource)
  }

  final class JStreamExtensions[A](val stream: JStream[A]) extends AnyVal {

    /**
      * Closes this stream when iteration is complete
      * It will NOT close the stream if it is not depleted!
      *
      * @return
      */
    def toAutoClosedIterator: Iterator[A] =
      stream.autoClosed.flatMap(_.iterator().asScala)
  }

  final private[files] class OrderingExtensions[A](val order: Ordering[A]) extends AnyVal {
    def andThenBy(order2: Ordering[A]): Ordering[A] =
      Ordering.comparatorToOrdering(order.thenComparing(order2))
  }

}

/**
  * Container for various implicits
  */
trait Implicits extends Dispose.FlatMap.Implicits with Scanner.Read.Implicits with Scanner.Source.Implicits {
  import Implicits._

  implicit def toStringInterpolations(sc: StringContext): StringInterpolations =
    new StringInterpolations(sc)

  implicit def toStringExtensions(str: String): StringExtensions =
    new StringExtensions(str)

  implicit def toFileExtensions(file: JFile): FileExtensions =
    new FileExtensions(file)

  implicit def toURLExtensions(url: URL): URLExtensions =
    new URLExtensions(url)

  implicit def toURIExtensions(uri: URI): URIExtensions =
    new URIExtensions(uri)

  implicit def toSymbolExtensions(symbol: Symbol): SymbolExtensions =
    new SymbolExtensions(symbol)

  implicit def toIteratorExtensions[A](it: Iterator[A]): IteratorExtensions[A] =
    new IteratorExtensions(it)

  implicit def toInputStreamExtensions(in: InputStream): InputStreamExtensions =
    new InputStreamExtensions(in)

  implicit def toDigestInputStreamExtensions(in: DigestInputStream): DigestInputStreamExtensions =
    new DigestInputStreamExtensions(in)

  implicit def toOutputStreamExtensions(out: OutputStream): OutputStreamExtensions =
    new OutputStreamExtensions(out)

  implicit def toPrintWriterExtensions(writer: PrintWriter): PrintWriterExtensions =
    new PrintWriterExtensions(writer)

  implicit def toReaderExtensions(reader: Reader): ReaderExtensions =
    new ReaderExtensions(reader)

  implicit def toBufferedReaderExtensions(reader: BufferedReader): BufferedReaderExtensions =
    new BufferedReaderExtensions(reader)

  implicit def toWriterExtensions(writer: Writer): WriterExtensions =
    new WriterExtensions(writer)

  implicit def toFileChannelExtensions(fc: FileChannel): FileChannelExtensions =
    new FileChannelExtensions(fc)

  implicit def toPathMatcherExtensions(matcher: PathMatcher): PathMatcherExtensions =
    new PathMatcherExtensions(matcher)

  implicit def toObjectInputStreamExtensions(ois: ObjectInputStream): ObjectInputStreamExtensions =
    new ObjectInputStreamExtensions(ois)

  implicit def toObjectOutputStreamExtensions(oos: ObjectOutputStream): ObjectOutputStreamExtensions =
    new ObjectOutputStreamExtensions(oos)

  implicit def toZipOutputStreamExtensions(out: ZipOutputStream): ZipOutputStreamExtensions =
    new ZipOutputStreamExtensions(out)

  implicit def toZipInputStreamExtensions(in: ZipInputStream): ZipInputStreamExtensions =
    new ZipInputStreamExtensions(in)

  implicit def toZipEntryExtensions(entry: ZipEntry): ZipEntryExtensions =
    new ZipEntryExtensions(entry)

  implicit def toDisposeableExtensions[A: Disposable](resource: A): DisposeableExtensions[A] =
    new DisposeableExtensions(resource)

  implicit def toJStreamExtensions[A](stream: JStream[A]): JStreamExtensions[A] =
    new JStreamExtensions(stream)

  private[files] implicit def toOrderingExtensions[A](order: Ordering[A]): OrderingExtensions[A] =
    new OrderingExtensions(order)

  implicit def stringToMessageDigest(algorithmName: String): MessageDigest =
    MessageDigest.getInstance(algorithmName)

  implicit def stringToCharset(charsetName: String): Charset =
    Charset.forName(charsetName)

  implicit def tokenizerToIterator(s: StringTokenizer): Iterator[String] =
    Iterator.continually(s.nextToken()).withHasNext(s.hasMoreTokens)

  //implicit def posixPermissionToFileAttribute(perm: PosixFilePermission) =
  //  PosixFilePermissions.asFileAttribute(Set(perm))

  private[files] implicit def pathStreamToFiles(files: JStream[Path]): Iterator[File] =
    files.toAutoClosedIterator.map(File.apply)
}
