package better

import java.io.{File => JFile, _}, StreamTokenizer.{TT_EOF => eof}
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.{Path, FileSystems}
import java.util.StringTokenizer
import java.util.stream.{Stream => JStream}
import java.util.zip.{GZIPInputStream, ZipEntry, ZipOutputStream, GZIPOutputStream}

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.io.{BufferedSource, Codec, Source}
import scala.util.Properties

package object files extends Implicits {
  type Files = Iterator[File]

  def root: File = FileSystems.getDefault.getRootDirectories.head
  def home: File = Properties.userHome.toFile
  def   ~ : File = home
  def  tmp: File = Properties.tmpDir.toFile
  val `..`: File => File = _.parent
  val  `.`: File => File = identity

  type Closeable = {
    def close(): Unit
  }

  type ManagedResource[A <: Closeable] = Traversable[A]

  // Some utils:
  @inline private[files] def when[A](condition: Boolean)(f: => A): Option[A] = if (condition) Some(f) else None
  @inline private[files] def repeat(n: Int)(f: => Unit): Unit = (1 to n).foreach(_ => f)
  @inline private[files] def returning[A](obj: A)(f: => Unit): A = {f; obj}
}
