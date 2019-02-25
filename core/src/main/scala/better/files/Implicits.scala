package better.files

import java.io.{File => JFile, _}
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.{Path, PathMatcher}
import java.security.MessageDigest
import java.util.StringTokenizer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.{Stream => JStream}
import java.util.zip._

import scala.annotation.tailrec
import scala.collection.JavaConverters._

/**
  * Container for various implicits
  */
trait Implicits extends Dispose.FlatMap.Implicits with Scanner.Read.Implicits with Scanner.Source.Implicits {

  //TODO: Rename all Ops to Extensions

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

    def inputStream(implicit charset: Charset = DefaultCharset): InputStream =
      new ByteArrayInputStream(str.getBytes(charset))

    def reader: Reader =
      new StringReader(str)
  }

  implicit class FileOps(file: JFile) {
    def toScala: File =
      File(file.getPath)
  }

  implicit class SymbolExtensions(symbol: Symbol) {
    def /(child: Symbol): File =
      File(symbol.name) / child
  }

  implicit class IteratorExtensions[A](it: Iterator[A]) {

    /**
      * Create an iterator which invokes onComplete() exactly once on exhaustion
      *
      * Note:
      *   This also makes certain operations exhaustive (e.g. take(), find(), head)
      *
      * @param it
      * @param onComplete
      * @tparam A
      * @return
      */
    def onComplete(f: => Unit): Iterator[A] =
      new Iterator[A] {
        val isOnCompleteInvoked = new AtomicBoolean(false)
        override def hasNext = {
          val hasNext = it.hasNext
          if (!hasNext && !isOnCompleteInvoked.getAndSet(true)) f
          hasNext
        }
        override def next() = it.next()
      }

    def withHasNext(f: => Boolean): Iterator[A] = new Iterator[A] {
      override def hasNext = f && it.hasNext
      override def next()  = it.next()
    }
  }

  implicit class InputStreamOps(in: InputStream) {
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

  implicit class OutputStreamOps(val out: OutputStream) {
    def buffered: BufferedOutputStream =
      new BufferedOutputStream(out)

    def buffered(bufferSize: Int): BufferedOutputStream =
      new BufferedOutputStream(out, bufferSize)

    def asGzipOutputStream(bufferSize: Int = DefaultBufferSize, syncFlush: Boolean = false): GZIPOutputStream =
      new GZIPOutputStream(out, bufferSize, syncFlush)

    def writer(implicit charset: Charset = DefaultCharset): OutputStreamWriter =
      new OutputStreamWriter(out, charset)

    def printWriter(autoFlush: Boolean = false): PrintWriter =
      new PrintWriter(out, autoFlush)

    def write(bytes: Iterator[Byte], bufferSize: Int = DefaultBufferSize): out.type = {
      bytes.grouped(bufferSize).foreach(buffer => out.write(buffer.toArray))
      out.flush()
      out
    }

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

  implicit class PrintWriterOps(pw: PrintWriter) {
    def printLines(lines: TraversableOnce[_]): PrintWriter = {
      lines.foreach(pw.println)
      pw
    }
  }

  implicit class ReaderOps(reader: Reader) {
    def buffered: BufferedReader =
      new BufferedReader(reader)

    def toInputStream(implicit charset: Charset = DefaultCharset): InputStream =
      new ReaderInputStream(reader)(charset)

    def chars: Iterator[Char] =
      new Dispose(reader).flatMap(res => eofReader(res.read()).map(_.toChar))
  }

  implicit class BufferedReaderOps(reader: BufferedReader) {
    def tokens(splitter: StringSplitter = StringSplitter.Default): Iterator[String] =
      reader.lines().toAutoClosedIterator.flatMap(splitter.split)
  }

  implicit class WriterOps(writer: Writer) {
    def buffered: BufferedWriter =
      new BufferedWriter(writer)

    def outputstream(implicit charset: Charset = DefaultCharset): OutputStream =
      new WriterOutputStream(writer)(charset)
  }

  implicit class FileChannelOps(fc: FileChannel) {
    def toMappedByteBuffer: MappedByteBuffer =
      fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())
  }

  implicit class PathMatcherOps(matcher: PathMatcher) {
    def matches(file: File, maxDepth: Int)(implicit visitOptions: File.VisitOptions = File.VisitOptions.default) =
      file.collectChildren(child => matcher.matches(child.path), maxDepth)(visitOptions)
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
      val entryName    = if (file.isDirectory) s"$relativeName/" else relativeName // make sure to end directories in ZipEntry with "/"
      out.putNextEntry(new ZipEntry(entryName))
      if (file.isRegularFile) file.inputStream.foreach(_.pipeTo(out))
      out.closeEntry()
      out
    }

    def +=(file: File): out.type =
      add(file, file.name)
  }

  implicit class ZipInputStreamOps(val in: ZipInputStream) {

    /**
      * Apply `f` on each ZipEntry in the archive, closing the entry after `f` has been applied.
      *
      * @param f The function to apply to each ZipEntry. Can fail if it returns a lazy value,
      *          like Iterator, as the entry will have been closed before the lazy value is evaluated.
      */
    def mapEntries[A](f: ZipEntry => A): Iterator[A] = new Iterator[A] {
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

  implicit class ZipEntryOps(val entry: ZipEntry) {

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

  implicit class DisposeableOps[A: Disposable](resource: A) {

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

  private[files] implicit def pathStreamToFiles(files: JStream[Path]): Iterator[File] =
    files.toAutoClosedIterator.map(File.apply)
}
