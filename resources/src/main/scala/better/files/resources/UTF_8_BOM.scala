/*
 * Copyright 2018 Frugal Mechanic (http://frugalmechanic.com)
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

import java.io.{OutputStream, Writer}
import java.nio.charset.{Charset, CharsetDecoder, CharsetEncoder}
import java.nio.charset.StandardCharsets.UTF_8

/**
  * This is a marker Charset that is used to write out UTF-8 BOM encoding in OutputStreamResource
  *
  * Originally I attempted to have the Charset directly encode the BOM (like the UTF-16 Charsets)
  * but ran into problems with not being able to call into protected methods of the UTF-8 Charset
  * implementation and did not want to copy/paste a bunch of code and/or implement a bunch of hacks
  * to make it work properly.
  */
object UTF_8_BOM extends Charset("UTF-8-BOM", Array("X-UTF-8-BOM")) {
  private val ByteOrderMarkChar: Char         = '\uFEFF' // Translates to 3 bytes when written as UTF-8: 0xEF 0xBB 0xBF
  private val ByteOrderMarkBytes: Array[Byte] = Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte)

  /**
    * Write the UTF-8 BOM Bytes (0xEF 0xBB 0xBF) to the OutputStream
    */
  def writeBOM(os: OutputStream): Unit = os.write(ByteOrderMarkBytes)

  /**
    * Write the UTF-8 BOM Char ('\uFEFF') to the Writer (which is assumed to be a UTF-8 Writer)
    */
  def writeBOM(w: Writer): Unit = w.append(ByteOrderMarkChar)

  override def contains(cs: Charset): Boolean = UTF_8.contains(cs)
  override def newDecoder(): CharsetDecoder   = UTF_8.newDecoder()
  override def newEncoder(): CharsetEncoder   = UTF_8.newEncoder()
}
