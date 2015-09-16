package better

import java.nio.file.FileAlreadyExistsException

import better.files._

import org.scalatest._

class FilesSpec extends FlatSpec with BeforeAndAfterEach with Matchers {
  var testRoot: File = _
  var fa: File = _
  var a1: File = _
  var a2: File = _
  var t1: File = _
  var t2: File = _
  var fb: File = _
  var b1: File = _
  var b2: File = _

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
    fa = testRoot/"a"
    a1 = testRoot/"a"/"a1"
    a2 = testRoot/"a"/"a2"
    t1 = testRoot/"a"/"a1"/"t1.txt"
    t2 = testRoot/"a"/"a1"/"t2.txt"
    fb = testRoot/"b"
    b1 = testRoot/"b"/"b1"
    b2 = testRoot/"b"/"b2.txt"
    Seq(a1, a2, fb).foreach(_.mkdirs())
    t1.touch()
    t2.touch()
  }

  override def afterEach() = testRoot.delete()

  "files" can "be instantiated" in {
    val f = File("/User/johndoe/Documents")
    val f1: File = file"/User/johndoe/Documents"
    val f2: File = root/"User"/"johndoe"/"Documents"
    val f3: File = home/"Documents"
    val f4: File = new java.io.File("/User/johndoe/Documents")
    val f5: File = "/User"/"johndoe"/"Documents"
    val f6: File = "/User/johndoe/Documents".toFile
    val f7: File = root/"User"/"johndoe"/"Documents"/"presentations" / `..`

    root.toString shouldEqual "/"
    home.toString.count(_ == '/') should be > 1
    (root/"usr"/"johndoe"/"docs").toString shouldEqual "/usr/johndoe/docs"
    Seq(f, f1, f2, f4, f5, f6, f7).map(_.toString).toSet shouldBe Set(f.toString)
  }

  it can "be matched" in {
    "src"/"test"/"foo" match {
      case SymbolicLink(to) => fail()   //this must be first case statement if you want to handle symlinks specially; else will follow link
      case Directory(children) => fail()
      case RegularFile(contents) => fail()
      case other if other.exists() => fail()  //A file may not be one of the above e.g. UNIX pipes, sockets, devices etc
      case _ =>                               //A file that does not exist
    }
    root/"dev"/"null" match {
      case SymbolicLink(to) => fail()
      case Directory(children) => fail()
      case RegularFile(contents) => fail()
      case other if other.exists() =>   //A file can be not any of the above e.g. UNIX pipes & sockets etc
      case _ => fail()
    }
    root/"dev" match {
      case Directory(children) => children.exists(_.path == "/dev/null") shouldBe true // /dev should have 'null'
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

    (testRoot/"diary")
      .createIfNotExists()
      .appendNewLine
      .appendLines("My name is", "Inigo Montoya")
      .readLines should contain theSameElementsInOrderAs Seq("", "My name is", "Inigo Montoya")
  }

  it should "glob" in {
    ("src"/"test").glob("**/*.scala").map(_.name) shouldEqual Seq("FilesSpec.scala")
    ("src"/"test").listRecursively().filter(_.extension contains ".scala") should have length 1
    ("src"/"test").list should have length 1
    ("src"/"test").listRecursively(maxDepth = 1) should have length 2
    ("src"/"test").listRecursively() should have length 4
  }

  it should "support names/extensions" in {
    fa.extension shouldBe None
    fa.nameWithoutExtension shouldBe fa.name
    t1.extension shouldBe Some(".txt")
    t1.name shouldBe "t1.txt"
    t1.nameWithoutExtension shouldBe "t1"
    t1.changeExtensionTo(".md").name shouldBe "t1.md"
    //t1.contentType shouldBe Some("txt")
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

    t1.addPermissions(PosixFilePermission.OWNER_EXECUTE)
    t1(PosixFilePermission.OWNER_EXECUTE) shouldBe true

    t1.removePermissions(PosixFilePermission.OWNER_EXECUTE)
    t1.isOwnerExecutable shouldBe false
  }

  it should "support equality" in {
    fa shouldEqual (testRoot/"a")
    fa shouldNot equal (testRoot/"b")
    //val c1 = fa.checksum()
    //fa.checksum() shouldEqual c1
    t1 < "hello"
    t2 < "hello"
    (t1 == t2) shouldBe false
    (t1 === t2) shouldBe true
    t2 < "hello world"
    (t1 == t2) shouldBe false
    (t1 === t2) shouldBe false
    //fa.checksum() should not equal c1
  }

  it should "support chown/chgrp" in {
    fa.owner.getName should not be empty
    //fa.chown("nobody").chgrp("nobody")
  }

  it should "support ln/cp/mv" in {
    val magicWord = "Hello World"
    t1 write magicWord
    // link
    b1.linkTo(a1, symbolic = true)
    b2.symLinkTo(t2)
    (b1 / "t1.txt").read() shouldEqual magicWord
    // copy
    b2.contents shouldBe empty
    t1.checksum() should not equal t2.checksum()
    a[FileAlreadyExistsException] should be thrownBy (t1 copyTo t2)
    t1.copyTo(t2, overwrite = true)
    t1.exists() shouldBe true
    t1.checksum() shouldEqual t2.checksum()
    b2.contents shouldEqual magicWord
    // rename
    t2.name shouldBe "t2.txt"
    t2.exists() shouldBe true
    val t3 = t2 renameTo "t3.txt"
    t3.name shouldBe "t3.txt"
    t2.exists() shouldBe false
    t3.exists() shouldBe true
    // move
    t3 moveTo t2
    t2.exists() shouldBe true
    t3.exists() shouldBe false
  }

  it should "do I/O via streams/writers" in {
    t1.reader
    //t1.in > t2.out
  }

  //TODO: Test above for all kinds of FileType
}
