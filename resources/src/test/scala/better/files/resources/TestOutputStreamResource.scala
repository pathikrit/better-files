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

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import java.io.ByteArrayInputStream
import java.nio.charset.{Charset, StandardCharsets}

final class TestOutputStreamResource extends AnyFunSuite with Matchers {
  // test(".tar.gz") { check("hello_world.txt.tar.gz") }
  // test(".tgz") { check("hello_world.txt.tgz") }
  // test(".tar.bz") { check("hello_world.txt.tar.bz") }
  // test(".tar.bz2") { check("hello_world.txt.tar.bz2") }
  // test(".tar.bzip2") { check("hello_world.txt.tar.bzip2") }
  // test(".tbz2") { check("hello_world.txt.tbz2") }
  // test(".tbz") { check("hello_world.txt.tbz") }
  // test(".tar.xz") { check("hello_world.txt.tar.xz") }
  // test(".tar") { check("hello_world.txt.tar") }
  test(".txt") { check("hello_world.txt") }
  test(".gz") { check("hello_world.txt.gz") }
  test(".bzip2") { check("hello_world.txt.bzip2") }
  test(".bz2") { check("hello_world.txt.bz2") }
  test(".bz") { check("hello_world.txt.bz") }
  test(".snappy") { check("hello_world.txt.snappy") }
  test(".xz") { check("hello_world.txt.xz") }
  test(".zip") { check("hello_world.txt.zip") }
  test(".jar") { check("hello_world.txt.jar") }
  test(".7z") { check("hello_world.txt.7z") }

  test("UTF-8-BOM Charset") {
    val bytes: Array[Byte] = writeBytes("foo.txt", UTF_8_BOM)

    bytes(0) shouldBe 0xef.toByte
    bytes(1) shouldBe 0xbb.toByte
    bytes(2) shouldBe 0xbf.toByte

    readToString("foo.txt", UTF_8_BOM, bytes) shouldBe testString
    readToString("foo.txt", StandardCharsets.UTF_8, bytes) shouldBe testString
    new String(bytes, StandardCharsets.UTF_8).head shouldBe '\uFEFF'
  }

  test("UTF-8-BOM Encoding") {
    val bytes: Array[Byte] = writeBytes("foo.txt", "UTF-8-BOM")

    bytes(0) shouldBe 0xef.toByte
    bytes(1) shouldBe 0xbb.toByte
    bytes(2) shouldBe 0xbf.toByte

    readToString("foo.txt", "UTF-8-BOM", bytes) shouldBe testString
    readToString("foo.txt", "UTF-8", bytes) shouldBe testString
    new String(bytes, StandardCharsets.UTF_8).head shouldBe '\uFEFF'
  }

  private val testString: String = "Hello World!\n oneByte: \u0024 twoByte: \u00A2 threeByte: \u20AC fourByteSupplementary: \uD83D\uDCA5"

  private def check(fileName: String): Unit = {
    checkCharset(fileName, StandardCharsets.UTF_8)
    checkCharset(fileName, StandardCharsets.UTF_16)
    checkCharset(fileName, StandardCharsets.UTF_16BE)
    checkCharset(fileName, StandardCharsets.UTF_16LE)
    checkCharset(fileName, UTF_8_BOM)
  }

  private def checkCharset(fileName: String, cs: Charset): Unit = {
    checkString(readToString(fileName, cs, writeBytes(fileName, cs)))
    checkString(readToString(fileName, cs.name, writeBytes(fileName, cs.name)))
  }

  private def writeBytes(fileName: String, cs: Charset): Array[Byte] = {
    val bos: ByteArrayOutputStream = new ByteArrayOutputStream()
    OutputStreamResource.wrap(bos, fileName = fileName).writer(cs).use { _.write(testString) }
    bos.toByteArray()
  }

  private def writeBytes(fileName: String, encoding: String): Array[Byte] = {
    val bos: ByteArrayOutputStream = new ByteArrayOutputStream()
    OutputStreamResource.wrap(bos, fileName = fileName).writer(encoding).use { _.write(testString) }
    bos.toByteArray()
  }

  private def readToString(fileName: String, cs: Charset, bytes: Array[Byte]): String = {
    val bis: ByteArrayInputStream = new ByteArrayInputStream(bytes)
    InputStreamResource.forInputStream(bis, fileName = fileName).readToString(cs)
  }

  private def readToString(fileName: String, encoding: String, bytes: Array[Byte]): String = {
    val bis: ByteArrayInputStream = new ByteArrayInputStream(bytes)
    InputStreamResource.forInputStream(bis, fileName = fileName).readToString(encoding)
  }

  private def checkString(s: String): Unit = {
    s shouldBe testString
  }
}
