package better

import better.files._

import java.nio.file.NoSuchFileException

import org.scalatest._

class FilesSpec extends FlatSpec with BeforeAndAfterEach with Matchers {
  var testRoot: File = _
  var fa: File = _
  var a1: File = _
  var a2: File = _
  var a11: File = _
  var a12: File = _
  var fb: File = _

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

  override def beforeEach() = {
    testRoot = File.newTempDir("better-files")
    fa = testRoot / "a"
    a1 = testRoot / "a" / "a1"
    a2 = testRoot / "a" / "a2"
    a11 = testRoot / "a" / "a1" / "a11.txt"
    a12 = testRoot / "a" / "a1" / "a21.txt"
    fb = testRoot / "b"
    Seq(a1, a2, fb).foreach(_.mkdirs())
    a11.touch()
    a12.touch()
  }

  override def afterEach() = testRoot.delete()

  "files" can "be instantiated" in {
    val f = File("/User/johndoe/Documents")
    val f1: File = file"/User/johndoe/Documents"
    val f2: File = root / "User" / "johndoe" / "Documents"
    val f3: File = home / "Documents"
    val f4: File = new java.io.File("/User/johndoe/Documents")
    val f5: File = "/User" / "johndoe" / "Documents"
    val f6: File = "/User/johndoe/Documents".toFile
    val f7: File = root / "User" / "johndoe" / "Documents" / "presentations" / `..`

    (root / "usr" / "johndoe" / "docs").toString shouldEqual "/usr/johndoe/docs"
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

  it should "glob" in {
    ("src" / "test").glob("**/*.scala").map(_.name) shouldEqual Seq("FilesSpec.scala")
    ("src" / "test").listRecursively().filter(_.extension contains ".scala") should have length 1
    ("src" / "test").list should have length 1
    ("src" / "test").listRecursively(maxDepth = 1) should have length 2
    ("src" / "test").listRecursively() should have length 4
  }

  "files" should "support names/extensions" in {
    fa.extension shouldBe None
    fa.nameWithoutExtension shouldBe fa.name
    a11.extension shouldBe Some(".txt")
    a11.name shouldBe "a11.txt"
    a11.nameWithoutExtension shouldBe "a11"
  }

  "files" should "have .size" in {
    a11.size shouldBe 0
    a11.write("Hello World")
    a11.size should be > 0L
    testRoot.size should be > (a11.size + a12.size)
  }

  it should "set/unset permissions" in {
    val file = a11

    import java.nio.file.attribute.PosixFilePermission
    assert(!file.permissions(PosixFilePermission.OWNER_EXECUTE))

    file += PosixFilePermission.OWNER_EXECUTE
    assert(file(PosixFilePermission.OWNER_EXECUTE))

    file -= PosixFilePermission.OWNER_EXECUTE
    assert(!file.isOwnerExecutable)
  }

  //TODO: Test above for all kinds of FileType
}
