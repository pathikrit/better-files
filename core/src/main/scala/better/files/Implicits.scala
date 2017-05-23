package better.files

import java.io.{File => JFile, _}, StreamTokenizer.{TT_EOF => eof}
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.{Path, PathMatcher}
import java.security.MessageDigest
import java.util.StringTokenizer
import java.util.stream.{Stream => JStream}
import java.util.zip._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Container for various implicits
  */
trait Implicits {

  implicit class StringInterpolations(sc: StringContext) {
    def file(args: Any*): File =
      value(args).toFile

    private[this] def value(args: Seq[Any]) =
      sc.s(args: _*)
  }

  implicit class StringOps(str: String) {
    def toFile: File =
      File(str)

    def /(child: String): File =
      toFile / child
  }

  implicit class FileOps(file: JFile) {
    def toScala: File =
      File(file.getPath)
  }

  //TODO: Rename all Ops to Extensions

  implicit class IteratorExtensions[A](it: Iterator[A]) {
    def withHasNext(f: => Boolean): Iterator[A] = new Iterator[A] {
      override def hasNext = f && it.hasNext
      override def next() = it.next()
    }
  }

  implicit class InputStreamOps(in: InputStream) {
    def pipeTo(out: OutputStream, bufferSize: Int = defaultBufferSize): Unit =
      pipeTo(out, Array.ofDim[Byte](bufferSize))

    /**
      * Pipe an input stream to an output stream using a byte buffer
      */
    @tailrec final def pipeTo(out: OutputStream, buffer: Array[Byte]): Unit = {
      val n = in.read(buffer)
      if (n > 0) {
        out.write(buffer, 0, n)
        pipeTo(out, buffer)
      }
    }

    def buffered: BufferedInputStream =
      new BufferedInputStream(in)

    def buffered(bufferSize: Int): BufferedInputStream =
      new BufferedInputStream(in, bufferSize)

    def gzipped: GZIPInputStream =
      new GZIPInputStream(in)

    def asObjectInputStream: ObjectInputStream =
      new ObjectInputStream(in)

    def reader(implicit charset: Charset = File.defaultCharset): InputStreamReader =
      new InputStreamReader(in, charset)

    def lines(implicit charset: Charset = File.defaultCharset): Iterator[String] =
      reader(charset).buffered.lines().toAutoClosedIterator

    def bytes: Iterator[Byte] =
      in.autoClosed.flatMap(res => Iterator.continually(res.read()).takeWhile(_ != eof).map(_.toByte))
  }

  implicit class OutputStreamOps(val out: OutputStream) {
    def buffered: BufferedOutputStream =
      new BufferedOutputStream(out)

    def gzipped: GZIPOutputStream =
      new GZIPOutputStream(out)

    def writer(implicit charset: Charset = File.defaultCharset): OutputStreamWriter =
      new OutputStreamWriter(out, charset)

    def printWriter(autoFlush: Boolean = false): PrintWriter =
      new PrintWriter(out, autoFlush)

    def write(bytes: Iterator[Byte], bufferSize: Int = defaultBufferSize): out.type = {
      bytes.grouped(bufferSize).foreach(buffer => out.write(buffer.toArray))
      out.flush()
      out
    }

    def tee(out2: OutputStream): OutputStream =
      new TeeOutputStream(out, out2)

    def asObjectOutputStream: ObjectOutputStream =
      new ObjectOutputStream(out)
  }

  implicit class ReaderOps(reader: Reader) {
    def buffered: BufferedReader =
      new BufferedReader(reader)
  }

  implicit class BufferedReaderOps(reader: BufferedReader) {
    def chars: Iterator[Char] =
      reader.autoClosed.flatMap(res => Iterator.continually(res.read()).takeWhile(_ != eof).map(_.toChar))

    private[files] def tokenizers(implicit config: Scanner.Config = Scanner.Config.default) =
      reader.lines().toAutoClosedIterator.map(line => new StringTokenizer(line, config.delimiter, config.includeDelimiters))

    def tokens(implicit config: Scanner.Config = Scanner.Config.default): Iterator[String] =
      tokenizers(config).flatMap(tokenizerToIterator)
  }

  implicit class WriterOps(writer: Writer) {
    def buffered: BufferedWriter =
      new BufferedWriter(writer)
  }

  implicit class FileChannelOps(fc: FileChannel) {
    def toMappedByteBuffer: MappedByteBuffer =
      fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())
  }

  implicit class PathMatcherOps(matcher: PathMatcher) {
    def matches(file: File)(implicit visitOptions: File.VisitOptions = File.VisitOptions.default) =
      file.collectChildren(child => matcher.matches(child.path))(visitOptions)
  }

  implicit class ObjectInputStreamOps(ois: ObjectInputStream) {
    def deserialize[A]: A =
      ois.readObject().asInstanceOf[A]
  }

  implicit class ObjectOutputStreamOps(val oos: ObjectOutputStream) {
    def serialize(obj: Serializable): oos.type = {
      oos.writeObject(obj)
      oos
    }
  }

  implicit class ZipOutputStreamOps(val out: ZipOutputStream) {

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
      val entryName = if (file.isDirectory) s"$relativeName/" else relativeName // make sure to end directories in ZipEntry with "/"
      out.putNextEntry(new ZipEntry(entryName))
      if (file.isRegularFile) file.inputStream.foreach(_.pipeTo(out))
      out.closeEntry()
      out
    }

    def +=(file: File): out.type =
      add(file, file.name)
  }

  implicit class ZipInputStreamOps(val in: ZipInputStream) {
    def mapEntries[A](f: ZipEntry => A): Iterator[A] = new Iterator[A] {
      private[this] var entry = in.getNextEntry

      override def hasNext = entry != null

      override def next() = {
        val result = Try(f(entry))
        Try(in.closeEntry())
        entry = in.getNextEntry
        result.get
      }
    }
  }

  implicit class ZipEntryOps(val entry: ZipEntry) {
    /**
      * Extract this ZipEntry under this rootDir
      *
      * @param rootDir directory under which this entry is extracted
      * @param inputStream use this inputStream when this entry is a file
      * @return the extracted file
      */
    def extractTo(rootDir: File, inputStream: => InputStream): File = {
      val child = rootDir.createChild(entry.getName, asDirectory = entry.isDirectory, createParents = true)
      if (!entry.isDirectory) child.outputStream.foreach(inputStream.pipeTo(_))
      child
    }
  }

  implicit class CloseableOps[A <: Closeable](resource: A) {
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
    def autoClosed: ManagedResource[A] =
      new ManagedResource(resource)(Disposable.closableDisposer)
  }

  implicit class JStreamOps[A](stream: JStream[A]) {
    /**
      * Closes this stream when iteration is complete
      * It will NOT close the stream if it is not depleted!
      *
      * @return
      */
    def toAutoClosedIterator: Iterator[A] =
      stream.autoClosed.flatMap(_.iterator().asScala)
  }

  private[files] implicit class OrderingOps[A](order: Ordering[A]) {
    def andThenBy(order2: Ordering[A]): Ordering[A] =
      Ordering.comparatorToOrdering(order.thenComparing(order2))
  }

  implicit def stringToMessageDigest(algorithmName: String): MessageDigest =
    MessageDigest.getInstance(algorithmName)

  implicit def stringToCharset(charsetName: String): Charset =
    Charset.forName(charsetName)

  implicit def tokenizerToIterator(s: StringTokenizer): Iterator[String] =
    Iterator.continually(s.nextToken()).withHasNext(s.hasMoreTokens)

  //implicit def posixPermissionToFileAttribute(perm: PosixFilePermission) =
  //  PosixFilePermissions.asFileAttribute(Set(perm))

  private[files] implicit def pathStreamToFiles(files: JStream[Path]): Files =
    files.toAutoClosedIterator.map(File.apply)
}
