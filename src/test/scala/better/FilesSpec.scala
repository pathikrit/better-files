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
   *    b1/ --> ../a1
   *    b2.txt --> ../a2/a22.txt
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
    val f = File("/User/johndoe/Documents")
    val f1: File = file"/User/johndoe/Documents"
    val f2: File = root / "User" / "johndoe" / "Documents"
    val f3: File = home / "Documents"
    val f4: File = new java.io.File("/User/johndoe/Documents")
    val f5: File = "src" / "test"
  }

  "file types" can "be matched" in {
    file"src/test/foo" match {
      case SymbolicLink(to) => fail()   //thus must be first case statement if you want to handle symlinks specially; else will follow link
      case Directory(children) => fail()
      case RegularFile(contents) => fail()
      case other if other.exists() => fail()  //A file can be not any of the above e.g. UNIX pipes & sockets etc
      case _ =>                               //A file that does not exist
    }
    //TODO: test for each of the above
  }

  it should "do basic I/O" in {
    a[NoSuchFileException] should be thrownBy a11.contents()
    a11 < "hello"
    a11.contents() shouldEqual "hello"
    a11 << "world"
    a11.contents() shouldEqual "helloworld\n"
    "foo" ->: a11
    "bar" >>: a11
    a11.contents() shouldEqual "foobar\n"
    a11.append("hello", "world")
    a11.contents() shouldEqual "foobar\nhello\nworld\n"
    (a12 << "hello" << "world").contents() shouldEqual "hello\nworld\n"
  }

  "paths" should "have dsl" in {
    (root / "usr" / "johndoe" / "docs").toString shouldEqual "/usr/johndoe/docs"
  }
}
