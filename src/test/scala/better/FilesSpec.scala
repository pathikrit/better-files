package better

import better.files._

import java.nio.file.NoSuchFileException

import org.scalatest._

class FilesSpec extends FlatSpec with BeforeAndAfter with Matchers {
  val testRoot = File.newTempDir("better-files")

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
    val f5: File = "/User" / "johndoe" / "Documents"
    val f6: File = "/User/johndoe/Documents".toFile
    val f7: File = root / "User" / "johndoe" / "Documents" / "presentations" / `..`

    Seq(f, f1, f2, f4, f5, f6, f7).map(_.toString).toSet shouldBe Set(f.toString)
  }

  "file types" can "be matched" in {
    "src" / "test" / "foo" match {
      case SymbolicLink(to) => fail()   //this must be first case statement if you want to handle symlinks specially; else will follow link
      case Directory(children) => fail()
      case RegularFile(contents) => fail()
      case other if other.exists() => fail()  //A file may not be one of the above e.g. UNIX pipes, sockets, devices etc
      case _ =>                               //A file that does not exist
    }
    root / "dev" / "null" match {
      case SymbolicLink(to) => fail()
      case Directory(children) => fail()
      case RegularFile(contents) => fail()
      case other if other.exists() =>   //A file can be not any of the above e.g. UNIX pipes & sockets etc
      case _ => fail()
    }
    root / "dev" match {
      case Directory(children) => children.exists(_.name == "null") shouldBe true // /dev should have 'null'
      case _ => fail()
    }
  }

  it should "do basic I/O" in {
    a[NoSuchFileException] should be thrownBy a11.read()
    a11 < "hello"
    a11.read() shouldEqual "hello"
    a11.appendNewLine << "world"
    (a11!) shouldEqual "hello\nworld\n"
    "foo" `>:` a11
    "bar" >>: a11
    a11.contents shouldEqual "foobar\n"
    a11.appendLines("hello", "world")
    a11.contents shouldEqual "foobar\nhello\nworld\n"
    a12.write("hello").appendNewLine.appendLines("world").read() shouldEqual "hello\nworld\n"
  }

  "paths" should "have dsl" in {
    (root / "usr" / "johndoe" / "docs").toString shouldEqual "/usr/johndoe/docs"
  }

  it should "glob" in {
    ("src" / "test").glob("**/*.scala").map(_.name) shouldEqual Seq("FilesSpec.scala")
    ("src" / "test").list should have length 1
    ("src" / "test").listRecursively should have length 4
  }

  it should "support file attribute APIs" in {
    fa.extension shouldBe None
    fa.nameWithoutExtension shouldBe fa.name
    a11.extension shouldBe Some(".txt")
    a11.name shouldBe "a11.txt"
    a11.nameWithoutExtension shouldBe "a11"
  }

  //TODO: Test above for all kinds of FileType
}
