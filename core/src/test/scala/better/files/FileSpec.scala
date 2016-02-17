package better.files

import File.{root, home}
import File.Type._
import Cmds._

import org.scalatest._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

class FileSpec extends FlatSpec with BeforeAndAfterEach with Matchers {
  val isCI = sys.env.get("CI").exists(_.toBoolean)

  def sleep(t: FiniteDuration = 2 second) = Thread.sleep(t.toMillis)

  var testRoot: File = _    //TODO: Get rid of mutable test vars
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
    testRoot = File.newTemporaryDirectory("better-files")
    fa = testRoot/"a"
    a1 = testRoot/"a"/"a1"
    a2 = testRoot/"a"/"a2"
    t1 = testRoot/"a"/"a1"/"t1.txt"
    t2 = testRoot/"a"/"a1"/"t2.txt"
    fb = testRoot/"b"
    b1 = testRoot/"b"/"b1"
    b2 = testRoot/"b"/"b2.txt"
    Seq(a1, a2, fb) foreach mkdirs
    Seq(t1, t2) foreach touch
  }

  override def afterEach() = rm(testRoot)

  override def withFixture(test: NoArgTest) = {
    val before = File.numberOfOpenFileDescriptors()
    val result = super.withFixture(test)
    val after = File.numberOfOpenFileDescriptors()
    assert(before == after, s"Resource leakage detected in $test")
    result
  }

  "files" can "be instantiated" in {
    import java.io.{File => JFile}

    val f = File("/User/johndoe/Documents")                      // using constructor
    val f1: File = file"/User/johndoe/Documents"                 // using string interpolator
    val f2: File = "/User/johndoe/Documents".toFile              // convert a string path to a file
    val f3: File = new JFile("/User/johndoe/Documents").toScala  // convert a Java file to Scala
    val f4: File = root/"User"/"johndoe"/"Documents"             // using root helper to start from root
    //val f5: File = `~` / "Documents"                             // also equivalent to `home / "Documents"`
    val f6: File = "/User"/"johndoe"/"Documents"                 // using file separator DSL
    val f7: File = home/"Documents"/"presentations"/`..`         // Use `..` to navigate up to parent
    val f8: File = root/"User"/"johndoe"/"Documents"/ `.`
    val f9: File = File(f.uri)

    root.toString shouldEqual "/"
    home.toString.count(_ == '/') should be > 1
    (root/"usr"/"johndoe"/"docs").toString shouldEqual "/usr/johndoe/docs"
    Seq(f, f1, f2, f4, /*f5,*/ f6, f8, f9).map(_.toString).toSet shouldBe Set(f.toString)
  }

  it can "be matched" in {
    "src"/"test"/"foo" match {
      case SymbolicLink(to) => fail()   //this must be first case statement if you want to handle symlinks specially; else will follow link
      case Directory(children) => fail()
      case RegularFile(contents) => fail()
      case other if other.exists => fail()  //A file may not be one of the above e.g. UNIX pipes, sockets, devices etc
      case _ =>                               //A file that does not exist
    }
    root/"dev"/"null" match {
      case SymbolicLink(to) => fail()
      case Directory(children) => fail()
      case RegularFile(contents) => fail()
      case other if other.exists =>   //A file can be not any of the above e.g. UNIX pipes & sockets etc
      case _ => fail()
    }
    root/"dev" match {
      case Directory(children) => children.exists(_.pathAsString == "/dev/null") shouldBe true // /dev should have 'null'
      case _ => fail()
    }
  }

  it should "do basic I/O" in {
    t1 < "hello"
    t1.contentAsString shouldEqual "hello"
    t1.appendNewLine << "world"
    (t1!) shouldEqual "hello\nworld\n"
    t1.chars.toStream should contain theSameElementsInOrderAs "hello\nworld\n".toSeq
    "foo" `>:` t1
    "bar" >>: t1
    t1.contentAsString shouldEqual "foobar\n"
    t1.appendLines("hello", "world")
    t1.contentAsString shouldEqual "foobar\nhello\nworld\n"
    t2.write("hello").append("world").contentAsString shouldEqual "helloworld"

    (testRoot/"diary")
      .createIfNotExists()
      .appendNewLine()
      .appendLines("My name is", "Inigo Montoya")
      .lines.toSeq should contain theSameElementsInOrderAs Seq("", "My name is", "Inigo Montoya")
  }

  it should "glob" in {
    a1.glob("**/*.txt").map(_.name).toSeq shouldEqual Seq("t1.txt", "t2.txt")
    //a1.glob("*.txt").map(_.name).toSeq shouldEqual Seq("t1.txt", "t2.txt")
    testRoot.glob("**/*.txt").map(_.name).toSeq shouldEqual Seq("t1.txt", "t2.txt")
    val path = testRoot.path.toString.ensuring(testRoot.path.isAbsolute)
    File(path).glob("**/*.{txt}").map(_.name).toSeq shouldEqual Seq("t1.txt", "t2.txt")
    ("benchmarks"/"src").glob("**/*.{scala,java}").map(_.name).toSeq shouldEqual Seq("ArrayBufferScanner.java", "Scanners.scala", "ScannerBenchmark.scala")
    ("benchmarks"/"src").glob("**/*.{scala}").map(_.name).toSeq shouldEqual Seq("Scanners.scala", "ScannerBenchmark.scala")
    ("benchmarks"/"src").glob("**/*.scala").map(_.name).toSeq shouldEqual Seq("Scanners.scala", "ScannerBenchmark.scala")
    ("benchmarks"/"src").listRecursively.filter(_.extension == Some(".scala")).map(_.name).toSeq shouldEqual Seq("Scanners.scala", "ScannerBenchmark.scala")
    ls("core"/"src"/"test") should have length 1
    ("core"/"src"/"test").walk(maxDepth = 1) should have length 2
    ("core"/"src"/"test").walk(maxDepth = 0) should have length 1
    ("core"/"src"/"test").walk() should have length (("core"/"src"/"test").listRecursively.length + 1L)
    ls_r("core"/"src"/"test") should have length 4
  }

  it should "support names/extensions" in {
    assume(isCI)
    fa.extension shouldBe None
    fa.nameWithoutExtension shouldBe fa.name
    t1.extension shouldBe Some(".txt")
    t1.name shouldBe "t1.txt"
    t1.nameWithoutExtension shouldBe "t1"
    t1.changeExtensionTo(".md").name shouldBe "t1.md"
    (t1 < "hello world").changeExtensionTo(".txt").name shouldBe "t1.txt"
    t1.contentType shouldBe Some("text/plain")
    ("src" / "test").toString should include ("better-files")
    (t1 == t1.toString) shouldBe false
    (t1.contentAsString == t1.toString) shouldBe false
    (t1 == t1.contentAsString) shouldBe false
    t1.root shouldEqual fa.root
    file"/tmp/foo.scala.html".extension shouldBe Some(".html")
    file"/tmp/foo.scala.html".nameWithoutExtension shouldBe "foo.scala"
  }

  it should "hide/unhide" in {
    t1.isHidden shouldBe false
  }

  it should "support parent/child" in {
    fa isChildOf testRoot shouldBe true
    testRoot isChildOf root shouldBe true
    root isChildOf root shouldBe true
    fa isChildOf fa shouldBe true
    b2 isChildOf b2 shouldBe false
    b2 isChildOf b2.parent shouldBe true
    root.parent shouldBe null
  }

  it should "support siblings" in {
    (file"/tmp/foo.txt" sibling "bar.txt").pathAsString shouldBe "/tmp/bar.txt"
    fa.siblings.toList.map(_.name) shouldBe List("b")
    fb isSiblingOf fa shouldBe true
  }

  it should "support sorting" in {
    testRoot.list.toSeq.sorted(File.Order.byName) should not be empty
    testRoot.list.toSeq.max(File.Order.bySize) should not be empty
    testRoot.list.toSeq.min(File.Order.byDepth) should not be empty
    testRoot.list.toSeq.min(File.Order.byModificationTime) should not be empty
    testRoot.list.toSeq.sorted(File.Order.byDirectoriesFirst) should not be empty
  }

  it must "have .size" in {
    fb.isEmpty shouldBe true
    t1.size shouldBe 0
    t1.write("Hello World")
    t1.size should be > 0L
    testRoot.size should be > (t1.size + t2.size)
  }

  it should "set/unset permissions" in {
    assume(isCI)
    import java.nio.file.attribute.PosixFilePermission
    //an[UnsupportedOperationException] should be thrownBy t1.dosAttributes
    t1.permissions()(PosixFilePermission.OWNER_EXECUTE) shouldBe false

    chmod_+(PosixFilePermission.OWNER_EXECUTE, t1)
    t1(PosixFilePermission.OWNER_EXECUTE) shouldBe true
    t1.permissionsAsString shouldBe "rwxrw-r--"

    chmod_-(PosixFilePermission.OWNER_EXECUTE, t1)
    t1.isOwnerExecutable shouldBe false
    t1.permissionsAsString shouldBe "rw-rw-r--"
  }

  it should "support equality" in {
    fa shouldEqual (testRoot/"a")
    fa shouldNot equal (testRoot/"b")
    val c1 = fa.md5
    fa.md5 shouldEqual c1
    t1 < "hello"
    t2 < "hello"
    (t1 == t2) shouldBe false
    (t1 === t2) shouldBe true
    t2 < "hello world"
    (t1 == t2) shouldBe false
    (t1 === t2) shouldBe false
    fa.md5 should not equal c1
  }

  it should "support chown/chgrp" in {
    fa.ownerName should not be empty
    fa.groupName should not be empty
    a[java.nio.file.attribute.UserPrincipalNotFoundException] should be thrownBy chown("hitler", fa)
    //a[java.nio.file.FileSystemException] should be thrownBy chown("root", fa)
    a[java.nio.file.attribute.UserPrincipalNotFoundException] should be thrownBy chgrp("cool", fa)
    //a[java.nio.file.FileSystemException] should be thrownBy chown("admin", fa)
    //fa.chown("nobody").chgrp("nobody")
    stat(t1) shouldBe a[java.nio.file.attribute.PosixFileAttributes]
  }

  it should "detect file locks" in {
    val file = File.newTemporaryFile()
    def lockInfo() = file.isReadLocked() -> file.isWriteLocked()
    // TODO: Why is file.isReadLocked() should be false?
    lockInfo() shouldBe (true -> false)
    val channel = file.newRandomAccess(File.RandomAccessMode.readWrite).getChannel
    val lock = channel.tryLock()
    lockInfo() shouldBe (true -> true)
    lock.release()
    channel.close()
    lockInfo() shouldBe (true -> false)
  }

  it should "support ln/cp/mv" in {
    val magicWord = "Hello World"
    t1 write magicWord
    // link
    b1.linkTo(a1, symbolic = true)
    ln_s(b2, t2)
    (b1 / "t1.txt").contentAsString shouldEqual magicWord
    // copy
    b2.contentAsString shouldBe empty
    t1.md5 should not equal t2.md5
    a[java.nio.file.FileAlreadyExistsException] should be thrownBy (t1 copyTo t2)
    t1.copyTo(t2, overwrite = true)
    t1.exists shouldBe true
    t1.md5 shouldEqual t2.md5
    b2.contentAsString shouldEqual magicWord
    // rename
    t2.name shouldBe "t2.txt"
    t2.exists shouldBe true
    val t3 = t2 renameTo "t3.txt"
    t3.name shouldBe "t3.txt"
    t2.exists shouldBe false
    t3.exists shouldBe true
    // move
    t3 moveTo t2
    t2.exists shouldBe true
    t3.exists shouldBe false
  }

  it should "support custom codec" in {
    import scala.io.Codec
    t1.write("你好世界")(codec = "UTF8")
    t1.contentAsString(Codec.ISO8859) should not equal "你好世界"
    t1.contentAsString(Codec.UTF8) shouldEqual "你好世界"
    val c1 = md5(t1)
    val c2 = t1.overwrite("你好世界")(File.OpenOptions.default, Codec.ISO8859).md5
    c1 should not equal c2
    c2 shouldEqual t1.checksum("md5")
  }

  it should "support hashing algos" in {
    t1.write("")
    assert(md5(t1) != sha1(t1))
    assert(sha256(t1) != sha512(t1))
  }

  it should "copy" in {
    (fb / "t3" / "t4.txt").createIfNotExists().write("Hello World")
    cp(fb / "t3", fb / "t5")
    (fb / "t5" / "t4.txt").contentAsString shouldEqual "Hello World"
    (fb / "t3").exists shouldBe true
  }

  it should "move" in {
    (fb / "t3" / "t4.txt").createIfNotExists().write("Hello World")
    mv(fb / "t3", fb / "t5")
    (fb / "t5" / "t4.txt").contentAsString shouldEqual "Hello World"
    (fb / "t3").notExists shouldBe true
  }

  it should "delete" in {
    fb.exists shouldBe true
    fb.delete()
    fb.exists shouldBe false
  }

  it should "touch" in {
    (fb / "z1").exists shouldBe false
    (fb / "z1").isEmpty shouldBe true
    (fb / "z1").touch()
    (fb / "z1").exists shouldBe true
    (fb / "z1").isEmpty shouldBe true
    Thread.sleep(1000)
    (fb / "z1").lastModifiedTime.getEpochSecond should be < (fb / "z1").touch().lastModifiedTime.getEpochSecond
  }

  it should "md5" in {
    val h1 = t1.hashCode
    val actual = (t1 < "hello world").md5
    val h2 = t1.hashCode
    h1 shouldEqual h2
    import scala.sys.process._, scala.language.postfixOps
    val expected = Try(s"md5sum ${t1.path}" !!) getOrElse (s"md5 ${t1.path}" !!)
    expected.toUpperCase should include (actual)
    actual should not equal h1
  }

  it should "support file in/out" in {
    t1 < "hello world"
    t1.newInputStream > t2.newOutputStream
    t2.contentAsString shouldEqual "hello world"
  }

  it should "zip/unzip directories" in {
    t1.write("hello world")
    val zipFile = testRoot.zip()
    zipFile.size should be > 100L
    zipFile.name should endWith (".zip")
    val destination = zipFile.unzip()
    (destination/"a"/"a1"/"t1.txt").contentAsString shouldEqual "hello world"
    destination === testRoot shouldBe true
    (destination/"a"/"a1"/"t1.txt").overwrite("hello")
    destination =!= testRoot shouldBe true
  }

  it should "zip/unzip single files" in {
    t1.write("hello world")
    val zipFile = t1.zip()
    zipFile.size should be > 100L
    zipFile.name should endWith (".zip")
    val destination = zipFile.unzip()
    (destination/"t1.txt").contentAsString shouldEqual "hello world"
  }

  it should "gzip" in {
    for {
      writer <- (testRoot / "test.gz").newOutputStream.buffered.gzipped.writer.buffered.autoClosed
    } writer.write("Hello world")

    (testRoot / "test.gz").inputStream.flatMap(_.buffered.gzipped.buffered.lines.toSeq) shouldEqual Seq("Hello world")
  }

  it should "read bytebuffers" in {
    t1.write("hello world")
    for {
      fileChannel <- t1.newFileChannel.autoClosed
      buffer = fileChannel.toMappedByteBuffer
    } buffer.remaining() shouldEqual t1.bytes.length
  }

  //TODO: Test above for all kinds of FileType

  "scanner" should "parse files" in {
    val data = t1 << s"""
    | Hello World
    | 1 2 3
    | Ok 23 football
    """.stripMargin
    val scanner: Scanner = data.newScanner()
    assert(scanner.lineNumber() == 0)
    assert(scanner.next[String] == "Hello")
    assert(scanner.lineNumber() == 2)
    assert(scanner.next[String] == "World")
    assert(scanner.next[Int] == 1)
    assert(scanner.next[Int] == 2)
    assert(scanner.lineNumber() == 3)
    assert(scanner.next[Int] == 3)
    assert(scanner.next[String] == "Ok")
    assert(scanner.tillEndOfLine() == " 23 football")
    assert(!scanner.hasNext)
    a[NoSuchElementException] should be thrownBy scanner.tillEndOfLine()
    a[NoSuchElementException] should be thrownBy scanner.next()
    assert(!scanner.hasNext)
    data.lineIterator.toSeq.filterNot(_.trim.isEmpty) shouldEqual data.newScanner.nonEmptyLines.toSeq
    data.tokens shouldEqual data.newScanner().toTraversable
  }

  it should "parse longs/booleans" in {
    val data = for {
      scanner <- Scanner("10 false").autoClosed
    } yield scanner.next[(Long, Boolean)]
    data shouldBe Seq(10L -> false)
  }

  it should "parse custom parsers" in {
    val file = t1 < """
      |Garfield
      |Woofer
    """.stripMargin

    sealed trait Animal
    case class Dog(name: String) extends Animal
    case class Cat(name: String) extends Animal

    implicit val animalParser: Scannable[Animal] = Scannable {scanner =>
      val name = scanner.next[String]
      if (name == "Garfield") Cat(name) else Dog(name)
    }
    val scanner = file.newScanner()
    Seq.fill(2)(scanner.next[Animal]) should contain theSameElementsInOrderAs Seq(Cat("Garfield"), Dog("Woofer"))
  }

  "file watcher" should "watch single files" in {
    assume(isCI)
    val file = File.newTemporaryFile(suffix = ".txt").write("Hello world")

    var log = List.empty[String]
    def output(msg: String) = synchronized {
      println(msg)
      log = msg :: log
    }
    /***************************************************************************/
    val watcher = new ThreadBackedFileMonitor(file) {
      override def onCreate(file: File) = output(s"$file got created")
      override def onModify(file: File) = output(s"$file got modified")
      override def onDelete(file: File) = output(s"$file got deleted")
    }
    watcher.start()
    /***************************************************************************/
    sleep(5 seconds)
    file.write("hello world"); sleep()
    file.clear(); sleep()
    file.write("howdy"); sleep()
    file.delete(); sleep()
    sleep(5 seconds)
    val sibling = (file.parent / "t1.txt").createIfNotExists(); sleep()
    sibling.write("hello world"); sleep()
    sleep(20 seconds)

    log.size should be >= 2
    log.exists(_ contains sibling.name) shouldBe false
    log.forall(_ contains file.name) shouldBe true
  }

  ignore should "watch directories to configurable depth" in {
    assume(isCI)
    val dir = File.newTemporaryDirectory()
    (dir/"a"/"b"/"c"/"d"/"e").createDirectories()
    var log = List.empty[String]
    def output(msg: String) = synchronized(log = msg :: log)

    val watcher = new ThreadBackedFileMonitor(dir, maxDepth = 2) {
      override def onCreate(file: File) = output(s"Create happened on ${file.name}")
    }
    watcher.start()

    sleep(5 seconds)
    (dir/"a"/"b"/"t1").touch().write("hello world"); sleep()
    (dir/"a"/"b"/"c"/"d"/"t1").touch().write("hello world"); sleep()
    sleep(10 seconds)

    withClue(log) {
      log.size shouldEqual 1
    }
  }
}
