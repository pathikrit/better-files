/*
 * Copyright 2015 Frugal Mechanic (http://frugalmechanic.com)
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
import java.io.{File, InputStream, InputStreamReader}

final class TestInputStreamResource extends AnyFunSuite with Matchers {
  private val rootDir: String = "better/files/resources"

  test(".tar.gz") { checkCompression("hello_world.txt.tar.gz", _.gunzip.untar) }
  test(".tgz") { checkCompression("hello_world.txt.tgz", _.gunzip.untar) }
  test(".tar.bz") { checkCompression("hello_world.txt.tar.bz", _.bunzip2.untar) }
  test(".tar.bz2") { checkCompression("hello_world.txt.tar.bz2", _.bunzip2.untar) }
  test(".tar.bzip2") { checkCompression("hello_world.txt.tar.bzip2", _.bunzip2.untar) }
  test(".tbz2") { checkCompression("hello_world.txt.tbz2", _.bunzip2.untar) }
  test(".tbz") { checkCompression("hello_world.txt.tbz", _.bunzip2.untar) }
  test(".tar.xz") { checkCompression("hello_world.txt.tar.xz", _.unxz.untar) }
  test(".tar") { checkCompression("hello_world.txt.tar", _.untar) }
  test(".gz") { checkCompression("hello_world.txt.gz", _.gunzip) }
  test(".bzip2") { checkCompression("hello_world.txt.bzip2", _.bunzip2) }
  test(".bz2") { checkCompression("hello_world.txt.bz2", _.bunzip2) }
  test(".bz") { checkCompression("hello_world.txt.bz", _.bunzip2) }
  test(".snappy") { checkCompression("hello_world.txt.snappy", _.unsnappy) }
  test(".xz") { checkCompression("hello_world.txt.xz", _.unxz) }
  test(".zip") { checkCompression("hello_world.txt.zip", _.unzip) }
  test(".jar") { checkCompression("hello_world.txt.jar", _.unjar) }
  //test(".7z") { checkCompression("hello_world.txt.7z", _.un7zip) }

  // test(".z7") {
  //   // .7z only works directly against a File so we have a special test for it
  //   InputStreamResource
  //     .forFile(new File(s"$rootDir/compression/hello_world.txt.7z"))
  //     .readToString("UTF-8") shouldBe "Hello World!\n"
  // }

  private def checkCompression(name: String, uncompress: InputStream => InputStream): Unit = {
    val file: File = new File(s"$rootDir/compression/$name")

    // Check via InputStreamResource
    InputStreamResource.forResource(file).readToString("UTF-8") shouldBe "Hello World!\n"

    // Check raw
    Resource.using(getClass.getClassLoader.getResourceAsStream(file.toString)) { raw: InputStream =>
      Resource.using(uncompress(raw)) { is: InputStream =>
        IOUtils.toString(new InputStreamReader(is, "UTF-8")) should equal("Hello World!\n")
      }
    }
  }

  /*
   * Test Reading from Different File Encodings
   */

  test("UTF-8 with BOM") { checkEncoding("quickbrown-UTF-8-with-BOM.txt") }
  test("UTF-8 no BOM") { checkEncoding("quickbrown-UTF-8-no-BOM.txt") }

  test("UTF-16BE with BOM") { checkEncoding("quickbrown-UTF-16BE-with-BOM.txt") }
  //test("UTF-16BE no BOM")   { checkEncoding("quickbrown-UTF-16BE-no-BOM.txt") }

  test("UTF-16LE with BOM") { checkEncoding("quickbrown-UTF-16LE-with-BOM.txt") }
  //test("UTF-16LE no BOM")   { checkEncoding("quickbrown-UTF-16LE-no-BOM.txt") }

  test("UTF-32BE with BOM") { checkEncoding("quickbrown-UTF-32BE-with-BOM.txt") }
  //test("UTF-32BE no BOM")   { checkEncoding("quickbrown-UTF-32BE-no-BOM.txt") }

  test("UTF-32LE with BOM") { checkEncoding("quickbrown-UTF-32LE-with-BOM.txt") }
  //test("UTF-32LE no BOM")   { checkEncoding("quickbrown-UTF-32LE-no-BOM.txt") }

  private val QuickBrownTest: String = {
    InputStreamResource.forResource(new File(s"$rootDir/encoding/quickbrown-UTF-8-no-BOM.txt")).readToString("UTF-8")
  }

  private def checkEncoding(file: String): Unit = {
    InputStreamResource.forResource(new File(s"$rootDir/encoding/$file")).readToString() should equal(QuickBrownTest)
  }
}
