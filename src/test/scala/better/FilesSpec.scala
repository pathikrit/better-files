package better

import better.files._

import java.nio.file.{Files, NoSuchFileException}

import org.scalatest._

class FilesSpec extends FlatSpec with BeforeAndAfter with Matchers {
  val root: File = Files.createTempDirectory("better-files")

  /**
   * Setup the following directory structure under root
   * /a
   * /a1
   * /a2
   * a21.txt
   * a22.txt
   * /b
   */
  val (fa, a1, a2, a11, a22, fb) = (
    root / "a",
    root / "a" / "a1",
    root / "a" / "a2",
    root / "a" / "a1" / "a11.txt",
    root / "a" / "a1" / "a21.txt",
    root / "b"
    )

  before {
    Seq(a1, a2, fb).foreach(_.mkdirs())
  }

  after {
    //root.de
  }

  "files" can "be instantiated" in {

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
  }
}
