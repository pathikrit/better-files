package better

import better.files._

import java.nio.file.NoSuchFileException

import org.scalatest._

class FilesSpec extends FlatSpec with BeforeAndAfterEach with Matchers {
  var testRoot: File = _
  var fa: File = _
  var a1: File = _
  var a2: File = _
  var t1: File = _
  var t2: File = _
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
    t1 = testRoot / "a" / "a1" / "t1.txt"
    t2 = testRoot / "a" / "a1" / "t2.txt"
    fb = testRoot / "b"
    Seq(a1, a2, fb).foreach(_.mkdirs())
    t1.touch()
    t2.touch()
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

  it can "be matched" in {
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
    t1 < "hello"
    t1.read() shouldEqual "hello"
    t1.appendNewLine << "world"
    (t1!) shouldEqual "hello\nworld\n"
    "foo" `>:` t1
    "bar" >>: t1
    t1.contents shouldEqual "foobar\n"
    t1.appendLines("hello", "world")
    t1.contents shouldEqual "foobar\nhello\nworld\n"
    t2.write("hello").appendNewLine.appendLines("world").read() shouldEqual "hello\nworld\n"
  }

  it should "glob" in {
    ("src" / "test").glob("**/*.scala").map(_.name) shouldEqual Seq("FilesSpec.scala")
    ("src" / "test").listRecursively().filter(_.extension contains ".scala") should have length 1
    ("src" / "test").list should have length 1
    ("src" / "test").listRecursively(maxDepth = 1) should have length 2
    ("src" / "test").listRecursively() should have length 4
  }

  it should "support names/extensions" in {
    fa.extension shouldBe None
    fa.nameWithoutExtension shouldBe fa.name
    t1.extension shouldBe Some(".txt")
    t1.name shouldBe "t1.txt"
    t1.nameWithoutExtension shouldBe "t1"
  }

  it must "have .size" in {
    t1.size shouldBe 0
    t1.write("Hello World")
    t1.size should be > 0L
    testRoot.size should be > (t1.size + t2.size)
  }

  it should "set/unset permissions" in {
    import java.nio.file.attribute.PosixFilePermission
    t1.permissions(PosixFilePermission.OWNER_EXECUTE) shouldBe false

    t1 += PosixFilePermission.OWNER_EXECUTE
    t1(PosixFilePermission.OWNER_EXECUTE) shouldBe true

    t1 -= PosixFilePermission.OWNER_EXECUTE
    t1.isOwnerExecutable shouldBe false
  }

  it should "support equality" in {
    fa shouldEqual (testRoot / "a")
    fa shouldNot equal (testRoot / "b")
  }

  it should "support chown/chgrp" in {
    //fa.chown("nobody").chgrp("nobody")
  }

  //TODO: Test above for all kinds of FileType
}
