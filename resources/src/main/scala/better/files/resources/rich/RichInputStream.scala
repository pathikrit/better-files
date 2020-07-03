/*
 * Copyright 2014 Frugal Mechanic (http://frugalmechanic.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package better.files.resources.rich

import java.io.{BufferedInputStream, File, InputStream}
import java.util.zip.GZIPInputStream

import org.apache.commons.compress.archivers.{ArchiveEntry, ArchiveInputStream, ArchiveStreamFactory}
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream

import better.files.resources.Snappy

/**
  * Rich wrapper for an InputStream
  *
  * Note:  This class has an optional dependency on Snappy Java (https://github.com/xerial/snappy-java)
  */
final class RichInputStream(val is: InputStream) extends AnyVal {

  def toBufferedInputStream: BufferedInputStream =
    is match {
      case buffered: BufferedInputStream => buffered
      case _                             => new BufferedInputStream(is)
    }

  def gunzip: InputStream   = new GZIPInputStream(is)
  def unsnappy: InputStream = Snappy.newInputStream(is)
  def bunzip2: InputStream  = new BZip2CompressorInputStream(is)
  def unxz: InputStream     = new XZCompressorInputStream(is)

  // This appears to be as fast as using ZipInputStream directly
  def unzip: InputStream  = unarchive(toBufferedInputStream, ArchiveStreamFactory.ZIP)
  def unjar: InputStream  = unarchive(toBufferedInputStream, ArchiveStreamFactory.JAR)
  def untar: InputStream  = unarchive(toBufferedInputStream, ArchiveStreamFactory.TAR)
  def un7zip: InputStream = unarchive(toBufferedInputStream, ArchiveStreamFactory.SEVEN_Z)

  /** For debugging archive files */
  def showArchiveEntries(): Unit = {
    val ais: ArchiveInputStream = new ArchiveStreamFactory().createArchiveInputStream(is)
    var e: ArchiveEntry         = ais.getNextEntry()
    while (e != null) {
      println(s"""name: "${e.getName}", isDirectory: ${e.isDirectory}, size: ${e.getSize}""")
      e = ais.getNextEntry()
    }
  }

  private def unarchive(is: BufferedInputStream, archiverName: String): InputStream = {
    // From http://commons.apache.org/proper/commons-compress/examples.html
    // The stream classes all wrap around streams provided by the calling code and they work on them directly without any additional buffering.
    // On the other hand most of them will benefit from buffering so it is highly recommended that users wrap their stream in Buffered(In|Out)putStreams before using the Commons Compress API.
    val ais: ArchiveInputStream = new ArchiveStreamFactory().createArchiveInputStream(archiverName, is)

    var entry: ArchiveEntry = null

    // We want to skip over any directory entries and also any OS X added ._{OriginalFileName} files
    // Also ignore the META-INF stuff in JAR files
    do {
      entry = ais.getNextEntry()
      require(entry != null, s"${archiverName.toUpperCase} Input Stream doesn't appear to have a file in it?")
    } while (entry.isDirectory || new File(entry.getName).getName.startsWith("._") || entry.getName.startsWith("META-INF/"))

    require(entry != null, s"${archiverName.toUpperCase} Input Stream doesn't appear to have a file in it?")

    ais
  }

}
