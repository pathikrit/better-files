package better

import better.files._

import java.nio.file.{Files, NoSuchFileException}

import org.scalatest._

class FilesSpec extends FlatSpec with BeforeAndAfter with Matchers {
  val testRoot: File = Files.createTempDirectory("better-files")

  /**
   * Setup the following directory structure under root
   * /a
   *  /a1
   *  /a2
   *    a21.txt
   *    a22.txt
   * /b
   */
  val (fa, a1, a2, a11, a12, fb) = (
    testRoot / "a",
    testRoot / "a" / "a1",
    testRoot / "a" / "a2",
    testRoot / "a" / "a1" / "a11.txt",
    testRoot / "a" / "a1" / "a21.txt",
    testRoot / "b"
  )

  before {
    Seq(a1, a2, fb).foreach(_.mkdirs())
  }

  after {
    //root.de
  }

  "files" can "be instantiated" in {
    val f1: File = file"/User/johndoe/Documents"
    val f2: File = root / "User" / "johndoe" / "Documents"
    val f3: File = home / "Documents"
    val f4: File = new java.io.File("/User/johndoe/Documents")
  }

  it should "do basic I/O" in {
    a[NoSuchFileException] should be thrownBy a11.contents()
    a11 < "hello"
    a11.contents() shouldEqual "hello"
    a11 << "world"
    a11.contents() shouldEqual "helloworld\n"
    a11 < "foo bar"
    a11.contents() shouldEqual "foo bar"
    a11.append("hello", "world")
    a11.contents() shouldEqual "foo barhello\nworld\n"
    (a12 << "hello" << "world").contents() shouldEqual "hello\nworld\n"
  }

  "paths" should "have dsl" in {
    (root / "usr" / "johndoe" / "docs").toString shouldEqual "/usr/johndoe/docs"
  }
}
