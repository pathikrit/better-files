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
package better.files.resources

import java.io._
import java.nio.charset.Charset
import org.apache.commons.io.{IOUtils => ApacheIOUtils}
import org.mozilla.universalchardet.UniversalDetector

/**
  * Collection of IO Utilities.  Some implemented via Apache Commons IO
  */
object IOUtils {

  /**
    * If this is already a BufferedInputStream return this otherwise wrap in a BufferedInputStream
    */
  def toBufferedInputStream(input: InputStream): BufferedInputStream =
    input match {
      case bufferedInput: BufferedInputStream => bufferedInput
      case _                                  => new BufferedInputStream(input)
    }

  /**
    * If this is already a BufferedOutputStream return this otherwise wrap in a BufferedOutputStream
    */
  def toBufferedOutputStream(output: OutputStream): BufferedOutputStream =
    output match {
      case bufferedOutput: BufferedOutputStream => bufferedOutput
      case _                                    => new BufferedOutputStream(output)
    }

  /**
    * If this is already a BufferedReader return this otherwise wrap in a BufferedReader
    */
  def toBufferedReader(input: Reader): BufferedReader =
    input match {
      case bufferedInput: BufferedReader => bufferedInput
      case _                             => new BufferedReader(input)
    }

  /**
    * If this is already a BufferedWriter return this otherwise wrap in a BufferedWriter
    */
  def toBufferedWriter(output: Writer): BufferedWriter =
    output match {
      case bufferedOutput: BufferedWriter => bufferedOutput
      case _                              => new BufferedWriter(output)
    }

  /**
    * Copy bytes from an Resource[InputStream] to an Resource[OutputStream].
    *
    * This method buffers the input internally, so there is no need to use a BufferedInputStream.
    */
  def copy(input: Resource[InputStream], output: Resource[OutputStream]): Int = Resource.use(input, output) { ApacheIOUtils.copy(_, _) }

  /**
    * Copy bytes from an InputStream to an OutputStream.
    *
    * This method buffers the input internally, so there is no need to use a BufferedInputStream.
    */
  def copy(input: InputStream, output: OutputStream): Int = ApacheIOUtils.copy(input, output)

  /**
    * Copy chars from a Reader to a Writer.
    *
    * This method buffers the input internally, so there is no need to use a BufferedReader.
    */
  def copy(input: Reader, output: Writer): Int = ApacheIOUtils.copy(input, output)

  /**
    * Read bytes from an input stream. This implementation guarantees that it will read as many bytes as possible before giving up;
    * this IS NOT ALWAYS the case for subclasses of InputStream.
    */
  def read(input: InputStream, buffer: Array[Byte]): Int = ApacheIOUtils.read(input, buffer)

  /**
    * Read bytes from an input stream. This implementation guarantees that it will read as many bytes as possible before giving up;
    * this IS NOT ALWAYS the case for subclasses of InputStream.
    */
  def read(input: InputStream, buffer: Array[Byte], offset: Int, length: Int): Int = ApacheIOUtils.read(input, buffer, offset, length)

  /**
    * Read characters from an input character stream. This implementation guarantees that it will read as many characters as possible before giving up;
    * this IS NOT ALWAYS the case for subclasses of Reader.
    */
  def read(input: Reader, buffer: Array[Char]): Int = ApacheIOUtils.read(input, buffer)

  /**
    * Read characters from an input character stream. This implementation guarantees that it will read as many characters as possible before giving up;
    * this IS NOT ALWAYS the case for subclasses of Reader.
    */
  def read(input: Reader, buffer: Array[Char], offset: Int, length: Int): Int = ApacheIOUtils.read(input, buffer, offset, length)

  /**
    * Skip bytes from an input byte stream. This implementation guarantees that it will read as many bytes as possible before giving up;
    * this IS NOT ALWAYS the case for subclasses of Reader.
    */
  def skip(input: InputStream, toSkip: Long): Long = ApacheIOUtils.skip(input, toSkip)

  /**
    * Skip characters from an input character stream. This implementation guarantees that it will read as many characters as possible before giving up;
    * this IS NOT ALWAYS the case for subclasses of Reader.
    */
  def skip(input: Reader, toSkip: Long): Long = ApacheIOUtils.skip(input, toSkip)

  /**
    * Get the contents of an InputStream as a byte[].
    *
    * This method buffers the input internally, so there is no need to use a BufferedInputStream.
    */
  def toByteArray(input: InputStream): Array[Byte] = ApacheIOUtils.toByteArray(input)

  /**
    * Get the contents of a Reader as a character array.
    *
    * This method buffers the input internally, so there is no need to use a BufferedReader.
    */
  def toCharArray(input: Reader): Array[Char] = ApacheIOUtils.toCharArray(input)

  /**
    * Get the contents of a Reader as a String.
    *
    * This method buffers the input internally, so there is no need to use a BufferedReader.
    */
  def toString(input: Reader): String = ApacheIOUtils.toString(input)

  /**
    * Attempt to detect the charset of the InputStream using the XML Detector or the Universal Detector
    */
  def detectCharset(is: InputStream, useMarkReset: Boolean): Option[Charset] = detectCharsetName(is, useMarkReset).map { Charset.forName }

  /**
    * Attempt to detect the charset of the InputStream using the XML Detector or the Universal Detector
    */
  def detectCharsetName(is: InputStream, useMarkReset: Boolean): Option[String] = {
    XMLUtil.detectXMLCharsetName(is, useMarkReset) orElse detectUniversalCharsetName(is, useMarkReset)
  }

  /**
    * Attempt to detect the charset of the InputStream using org.mozilla.universalchardet.UniversalDetector
    */
  def detectUniversalCharset(is: InputStream, useMarkReset: Boolean): Option[Charset] =
    detectUniversalCharsetName(is, useMarkReset).map { Charset.forName }

  /**
    * Attempt to detect the charset of the InputStream using org.mozilla.universalchardet.UniversalDetector
    */
  def detectUniversalCharsetName(is: InputStream, useMarkReset: Boolean): Option[String] = {
    if (useMarkReset) require(is.markSupported, "detectCharsetName required mark/reset support!  Try wrapping in a BufferedInputStream")

    val bufSize: Int = 8192

    if (useMarkReset) is.mark(bufSize)

    val buf: Array[Byte]            = new Array[Byte](bufSize)
    val detector: UniversalDetector = new UniversalDetector(null)

    // Bytes read per-read (can be <= bufSize)
    var nread: Int = read(is, buf)

    if (useMarkReset) {
      // We can only feed as much data as we've mark()'ed
      detector.handleData(buf, 0, nread)
    } else {
      // We can read as much data as we need
      // NOTE!!! Apache IOUtils.read will read as much data as possible and will not return -1 for EOF!!!
      while (nread > 0) {
        detector.handleData(buf, 0, nread)
        nread = if (detector.isDone) -1 else read(is, buf)
      }
    }

    detector.dataEnd()

    val encoding: String = detector.getDetectedCharset()
    detector.reset()

    if (useMarkReset) is.reset()

    if (null != encoding && Charset.isSupported(encoding)) Some(encoding) else None
  }
}
