package better.files

import java.io.{File => JFile, _}, StreamTokenizer.{TT_EOF => eof}
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.StringTokenizer
import java.util.stream.{Stream => JStream}
import java.util.zip.{Deflater, GZIPInputStream, ZipEntry, ZipOutputStream, GZIPOutputStream}

import scala.annotation.tailrec
import scala.io.{BufferedSource, Codec, Source}
import scala.util.control.NonFatal

/**
  * Container for various implicits
  */
trait Implicits {

  implicit class StringInterpolations(sc: StringContext) {
    def file(args: Any*): File =
      value(args).toFile

    def resource(args: Any*): Source =
      Source.fromInputStream(getClass.getResourceAsStream(value(args)))

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

  implicit class InputStreamOps(in: InputStream) {
    def >(out: OutputStream): Unit =
      pipeTo(out)

    def pipeTo(out: OutputStream, closeOutputStream: Boolean = true, bufferSize: Int = 1 << 10): Unit =
      pipeTo(out, closeOutputStream, Array.ofDim[Byte](bufferSize))

    /**
      * Pipe an input stream to an output stream using a byte buffer
      */
    @tailrec final def pipeTo(out: OutputStream, closeOutputStream: Boolean, buffer: Array[Byte]): Unit = {
      in.read(buffer) match {
        case n if n > 0 =>
          out.write(buffer, 0, n)
          pipeTo(out, closeOutputStream, buffer)
        case _ =>
          in.close()
          if (closeOutputStream) out.close()
      }
    }

    def buffered: BufferedInputStream =
      new BufferedInputStream(in)

    def gzipped: GZIPInputStream =
      new GZIPInputStream(in)

    def reader(implicit codec: Codec): InputStreamReader =
      new InputStreamReader(in, codec)

    def content(implicit codec: Codec): BufferedSource =
      Source.fromInputStream(in)(codec)

    def lines(implicit codec: Codec): Iterator[String] =
      content(codec).getLines()

    def bytes: Iterator[Byte] =
      in.autoClosedIterator(_.read())(_ != eof).map(_.toByte)
  }

  implicit class OutputStreamOps(val out: OutputStream) {
    def buffered: BufferedOutputStream =
      new BufferedOutputStream(out)

    def gzipped: GZIPOutputStream =
      new GZIPOutputStream(out)

    def writer(implicit codec: Codec): OutputStreamWriter =
      new OutputStreamWriter(out, codec)

    def printWriter(autoFlush: Boolean = false): PrintWriter =
      new PrintWriter(out, autoFlush)

    def write(bytes: Iterator[Byte], bufferSize: Int = 1 << 10): out.type = {
      bytes grouped bufferSize foreach { buffer => out.write(buffer.toArray) }
      out.flush()
      out
    }

    def tee(out2: OutputStream): OutputStream = new OutputStream {
      override def write(b: Int): Unit = {
        out.write(b)
        out2.write(b)
      }

      override def flush() = {
        out.flush()
        out2.flush()
      }

      override def write(b: Array[Byte]) = {
        out.write(b)
        out2.write(b)
      }

      override def write(b: Array[Byte], off: Int, len: Int) = {
        out.write(b, off, len)
        out2.write(b, off, len)
      }

      override def close() = {
        out.close()
        out2.close()
      }
    }
  }

  implicit class ReaderOps(reader: Reader) {
    def buffered: BufferedReader =
      new BufferedReader(reader)
  }

  implicit class BufferedReaderOps(reader: BufferedReader) {
    def chars: Iterator[Char] =
      reader.autoClosedIterator(_.read())(_ != eof).map(_.toChar)

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
      if (file.isRegularFile) file.newInputStream.pipeTo(out, closeOutputStream = false)
      out.closeEntry()
      out
    }

    def +=(file: File): out.type =
      add(file, file.name)
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
    def autoClosed: ManagedResource[A] = new Traversable[A] {
      var isClosed = false
      override def foreach[U](f: A => U) = try {
        f(resource)
      } finally {
        if (!isClosed) {
          resource.close()
          isClosed = true
        }
      }
    }

    /**
      * Provides an iterator that closes the underlying resource when done
      *
      * e.g.
      * <pre>
      * inputStream.autoClosedIterator(_.read())(_ != -1).map(_.toByte)
      * </pre>
      *
      * @param generator      next element from this resource
      * @param isValidElement a function which tells if there is no more B left e.g. certain iterators may return nulls
      * @tparam B
      * @return An iterator that closes the underlying resource when done
      */
    def autoClosedIterator[B](generator: A => B)(isValidElement: B => Boolean): Iterator[B] = {
      var isClosed = false
      def isOpen(item: B) = {
        if (!isClosed && !isValidElement(item)) close()
        !isClosed
      }

      def close() = try {
        if (!isClosed) resource.close()
      } finally {
        isClosed = true
      }

      def next() = try {
        generator(resource)
      } catch {
        case NonFatal(e) =>
          close()
          throw e
      }

      Iterator.continually(next()).takeWhile(isOpen)
    }
  }

  implicit class JStreamOps[A](stream: JStream[A]) {
    /**
      * Closes this stream when iteration is complete
      * It will NOT close the stream if it is not depleted!
      *
      * @return
      */
    def toAutoClosedIterator: Iterator[A] = {
      val iterator = stream.iterator()
      var isOpen = true
      produce(iterator.next()) till {
        if (isOpen && !iterator.hasNext) {
          try {
            stream.close()
          } finally {
            isOpen = false
          }
        }
        isOpen
      }
    }
  }

  implicit def tokenizerToIterator(s: StringTokenizer): Iterator[String] =
    produce(s.nextToken()).till(s.hasMoreTokens)

  implicit def codecToCharSet(codec: Codec): Charset =
    codec.charSet

  //implicit def posixPermissionToFileAttribute(perm: PosixFilePermission) =
  //  PosixFilePermissions.asFileAttribute(Set(perm))

  private[files] implicit def pathStreamToFiles(files: JStream[Path]): Files =
    files.toAutoClosedIterator.map(File.apply)
}
