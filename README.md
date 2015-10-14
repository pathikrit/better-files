# better-files [![CircleCI][circleCiImg]][circleCiLink] [![Codacy][codacyImg]][codacyLink] [![Gitter][gitterImg]][gitterLink]

`better-files` is a [dependency-free](build.sbt) *pragmatic* [thin Scala wrapper](core/src/main/scala/better/files/File.scala) around [Java NIO](https://docs.oracle.com/javase/tutorial/essential/io/fileio.html).

## Tutorial
  0. [Instantiation](#instantiation)
  0. [Simple I/O](#file-readwrite)
  0. [Streams and Codecs](#streams-and-codecs)
  0. [Java compatibility](#java-interoperability)
  0. [Pattern matching](#pattern-matching)
  0. [Globbing](#globbing)
  0. [File system operations](#file-system-operations)
  0. [UNIX DSL](#unix-dsl)
  0. [File attributes](#file-attributes)
  0. [File comparison](#file-comparison)
  0. [Zip/Unzip](#zip-apis)
  0. [Automatic Resource Management](#lightweight-arm)
  0. [Scanner] (#scanner)
  0. [File Monitoring](#file-monitoring)
  0. [Reactive File Watcher](#akka-file-watcher)

## [ScalaDoc](http://pathikrit.github.io/better-files/latest/api/#better.files.package$$File)

## [Tests](src/test/scala/better/FilesSpec.scala) [![codecov][codecovImg]][codecovLink]

## sbt [![VersionEye][versionEyeImg]][versionEyeLink]
In your `build.sbt`, add this (compatible with [both Scala 2.10 and 2.11](https://bintray.com/pathikrit/maven/better-files#files)):
```scala
resolvers += Resolver.bintrayRepo("pathikrit", "maven")

libraryDependencies += "com.github.pathikrit" %% "better-files" % version
```
Latest `version`: [![Bintray][bintrayImg]][bintrayLink]

[circleCiImg]: https://img.shields.io/circleci/project/pathikrit/better-files/master.svg
[circleCiLink]: https://circleci.com/gh/pathikrit/better-files

[codecovImg]: https://img.shields.io/codecov/c/github/pathikrit/better-files/master.svg
[codecovLink]: http://codecov.io/github/pathikrit/better-files?branch=master

[versionEyeImg]: https://www.versioneye.com/user/projects/55f5e7de3ed894001e0003b1/badge.svg?style=flat
[versionEyeLink]: https://www.versioneye.com/user/projects/55f5e7de3ed894001e0003b1

[codacyImg]: https://img.shields.io/codacy/0e2aeb7949bc49e6802afcc43a7a1aa1.svg
[codacyLink]: https://www.codacy.com/app/pathikrit/better-files/dashboard

[bintrayImg]: https://img.shields.io/bintray/v/pathikrit/maven/better-files.svg
[bintrayLink]: https://bintray.com/pathikrit/maven/better-files

[gitterImg]: https://badges.gitter.im/Join%20Chat.svg
[gitterLink]: https://gitter.im/pathikrit/better-files

--- 
### Instantiation 
The following are all equivalent:
```scala
import better.files._
import java.io.{File => JFile}

val f = File("/User/johndoe/Documents")                      // using constructor
val f1: File = file"/User/johndoe/Documents"                 // using string interpolator
val f2: File = "/User/johndoe/Documents".toFile              // convert a string path to a file
val f3: File = new JFile("/User/johndoe/Documents").toScala  // convert a Java file to Scala
val f4: File = root/"User"/"johndoe"/"Documents"             // using root helper to start from root
val f5: File = `~` / "Documents"                             // also equivalent to `home / "Documents"`
val f6: File = "/User"/"johndoe"/"Documents"                 // using file separator DSL
val f7: File = home/"Documents"/"presentations"/`..`         // Use `..` to navigate up to parent
```
Resources in the classpath can be accessed using resource interpolator e.g. `resource"production.config"` 

**Note**: Rename the import if you think the usage of the class `File` may confuse your teammates:
```scala
import better.files.{File => BetterFile, _}
import java.io.File
```
I personally prefer renaming the Java crap instead:
```scala
import better.files._
import java.io.{File => JFile}
```

### File Read/Write
Dead simple I/O:
```scala
val file = root/"tmp"/"test.txt"
file.overwrite("hello")
file.appendNewLine().append("world")
assert(file.contentAsString == "hello\nworld")
```
If you are someone who likes symbols, then the above code can also be written as:
```scala
file < "hello"     // same as file.overwrite("hello")
file << "world"    // same as file.appendLines("world")
assert(file! == "hello\nworld")
```
Or even, right-associatively:
```scala
"hello" >: file
"world" >>: file
val bytes: Array[Byte] = file.loadBytes
```
All operations are chainable e.g.
```scala
 (root/"tmp"/"diary.txt")
  .createIfNotExists()  
  .appendNewLine
  .appendLines("My name is", "Inigo Montoya")
  .moveTo(home/"Documents")
  .renameTo("princess_diary.txt")
  .changeExtensionTo(".md")
  .lines
```

### Streams and Codecs
Various ways to slurp a file without loading the contents into memory:
 ```scala
val bytes  : Iterator[Byte]            = file.bytes
val chars  : Iterator[Char]            = file.chars
val lines  : Iterator[String]          = file.lines
val source : scala.io.BufferedSource   = file.newBufferedSource // needs to be closed, unlike the above APIs which auto closes when iterator ends
```
Note: The above APIs can be traversed atmost once e.g. `file.bytes` is a `Iterator[Byte]` which only allows `TraversableOnce`. 
To traverse it multiple times without creating a new iterator instance, convert it into some other collection e.g. `file.bytes.toStream`

You can supply your own codec too for anything that does a read/write (it assumes `scala.io.Codec.default` if you don't provide one):
```scala
val content: String = file.contentAsString  // default codec
// custom codec:
import scala.io.Codec
file.contentAsString(Codec.ISO8859)
//or
import scala.io.Codec.string2codec
file.write("hello world")(codec = "US-ASCII")
 ```
 
### Java interoperability
You can always access the Java I/O classes:
```scala
val file: File = tmp / "hello.txt"
val javaFile     : java.io.File                 = file.toJava
val uri          : java.net.uri                 = file.uri
val reader       : java.io.BufferedReader       = file.newBufferedReader 
val outputstream : java.io.OutputStream         = file.newOutputStream 
val writer       : java.io.BufferedWriter       = file.newBufferedWriter 
val inputstream  : java.io.InputStream          = file.newInputStream
val path         : java.nio.file.Path           = file.path
val fs           : java.nio.file.FileSystem     = file.fileSystem
val channel      : java.nio.channel.FileChannel = file.newFileChannel
val ram          : java.io.RandomAccessFile     = file.newRandomAccess
val fr           : java.io.FileReader           = file.newFileReader
val fw           : java.io.FileWriter           = file.newFileWriter(append = true)
```
The library also adds some useful implicits to above classes e.g.:
```scala
file1.reader > file2.writer       // pipes a reader to a writer
System.in > file2.out             // pipes an inputstream to an outputstream
src.pipeTo(sink)                  // if you don't like symbols

val bytes   : Iterator[Byte]        = inputstream.bytes
val bis     : BufferedInputStream   = inputstream.buffered  
val bos     : BufferedOutputStream  = outputstream.buffered   
val reader  : InputStreamReader     = inputstream.reader
val writer  : OutputStreamWriter    = outputstream.writer
val printer : PrintWriter           = outputstream.printer
val br      : BufferedReader        = reader.buffered
val bw      : BufferedWriter        = writer.buffered
val mm      : MappedByteBuffer      = fileChannel.toMappedByteBuffer
```
 
### Pattern matching
Instead of `if-else`, more idiomatic powerful Scala pattern matching:
```scala
/**
 * @return true if file is a directory with no children or a file with no contents
 */
def isEmpty(file: File): Boolean = file match {
  case SymbolicLink(to) => isEmpty(to)  // this must be first case statement if you want to handle symlinks specially; else will follow link
  case Directory(files) => files.isEmpty
  case RegularFile(content) => content.isEmpty
  case _ => file.notExists    // a file may not be one of the above e.g. UNIX pipes, sockets, devices etc
}
// or as extractors on LHS:
val Directory(researchDocs) = home/"Downloads"/"research"
```

### Globbing
No need to port [this](http://docs.oracle.com/javase/tutorial/essential/io/find.html) to Scala:
```scala
val dir = "src"/"test"
val matches: Seq[File] = dir.glob("**/*.{java,scala}")
// above code is equivalent to:
dir.listRecursively.filter(f => f.extension == Some(".java") || f.extension == Some(".scala")) 
```
You can even use more advanced regex syntax instead of glob syntax:
```scala
val matches = dir.glob("^\\w*$", syntax = "regex")
```
For simpler cases, you can always use `dir.list` or `dir.walk(maxDepth: Int)`

### File system operations
Utilities to `ls`, `cp`, `rm`, `mv`, `ln`, `md5`, `diff`, `touch`, `cat` etc:
```scala
file.touch()
file.delete()     // unlike the Java API, also works on directories as expected (deletes children recursively)
file.clear()      // If directory, deletes all children; if file clears contents
file.renameTo(newName: String)
file.moveTo(destination)
file.copyTo(destination)       // unlike the default API, also works on directories (copies recursively)
file.linkTo(destination)                     // ln file destination
file.symbolicLinkTo(destination)             // ln -s file destination
file.{checksum, md5, digest}   // also works for directories
file.setOwner(user: String)    // chown user file
file.setGroup(group: String)   // chgrp group file
Seq(file1, file2) >: file3     // same as cat file1 file2 > file3
Seq(file1, file2) >>: file3    // same as cat file1 file2 >> file3
```

### UNIX DSL
All the above can also be expressed using [methods](http://pathikrit.github.io/better-files/latest/api/#better.files.package$$Cmds$) reminiscent of the command line:
```scala
import better.files_, Cmds._   // must import Cmds._ to bring in these utils
cp(file1, file2)
mv(file1, file2)
rm(file) /*or*/ del(file)
ls(file) /*or*/ dir(file)
ln(file1, file2)     // hard link
ln_s(file1, file2)   // soft link
cat(file1)
cat(file1) >>: file
touch(file)
mkdir(file)
mkdirs(file)         // mkdir -p
chown(owner, file)
chgrp(owner, file)
chmod_+(permission, files)  // add permission
chmod_-(permission, files)  // remove permission
md5(file)
unzip(zipFile)(targetDir)
zip(file*)(zipFile)
```

### File attributes
Query various file attributes e.g.:
```scala
file.name       // simpler than java.io.File#getName
file.extension
file.contentType
file.lastModifiedTime     // returns JSR-310 time
file.owner / file.group
file.isDirectory / file.isSymbolicLink / file.isRegularFile
file.isHidden
file.hide() / file.unhide()
file.isOwnerExecutable / file.isGroupReadable // etc. see file.permissions
file.size                 // for a directory, computes the directory size
file.posixAttributes / file.dosAttributes  // see file.attributes
file.isEmpty      // true if file has no content (or no children if directory) or does not exist
```
`chmod`:
```scala
import java.nio.file.attribute.PosixFilePermission
file.addPermission(PosixFilePermission.OWNER_EXECUTE)      // chmod +X file
file.removePermission(PosixFilePermission.OWNER_WRITE)     // chmod -w file

// The following are all equivalent:
assert(file.permissions contains PosixFilePermission.OWNER_EXECUTE)
assert(file(PosixFilePermission.OWNER_EXECUTE))
assert(file.isOwnerExecutable)
```

### File comparison
Use `==` to check for path-based equality and `===` for content-based equality:
```scala
file1 == file2    // equivalent to `file1.isSamePathAs(file2)`
file1 === file2   // equivalent to `file1.isSameContentAs(file2)` (works for regular-files and directories)
file1 != file2    // equivalent to `!file1.isSamePathAs(file2)`
file1 =!= file2   // equivalent to `!file1.isSameContentAs(file2)`
```

### Zip APIs
You don't have to lookup on StackOverflow "[How to zip/unzip in Java/Scala?](http://stackoverflow.com/questions/9324933/)":
```scala
// Unzipping:
val zipFile: File = file"path/to/research.zip"
val research: File = zipFile.unzipTo(destination = home/"Documents"/"research") 

// Zipping:
val zipFile: File = directory.zipTo(destination = home/"Desktop"/"toEmail.zip")

// Zipping/Unzipping to temporary files/directories:
val someTempZipFile: File = directory.zip()
val someTempDir: File = zipFile.unzip()
assert(directory === someTempDir)

// Gzip handling:
File("countries.gz").newInputStream.gzipped.lines.take(10).foreach(println)
```

### Lightweight ARM
Auto-close Java closeables:
```scala
for {
  in <- file1.newInputStream.autoClosed
  out <- file2.newOutputStream.autoClosed
} in.pipeTo(out)
// The input and output streams are auto-closed once out of scope
```
`better-files` provides convenient managed versions of all the Java closeables e.g. instead of writing:
```scala
for {
 reader <- file.newBufferedReader.autoClosed
} foo(reader)
```
You can write:
```scala
for {
 reader <- file.bufferedReader    // returns ManagedResource[BufferedReader]
} foo(reader)

// or simply:
file.bufferedReader.map(foo)
```
One another [utility to convert any closeable to an iterator](http://pathikrit.github.io/better-files/latest/api/#better.files.package$$CloseableOps):
```scala
val eof = -1
val bytes: Iterator[Byte] = inputStream.autoClosedIterator(_.read())(_ != eof).map(_.toByte) 
```
Note: The `autoClosedIterator` only closes the resource when `hasNext` i.e. `(_ != eof)` returns false. 
If you only partially use the iterator e.g. `.take(5)`, it may leave the resource open. In those cases, use the managed `autoClosed` version instead.

### Scanner
Although [`java.util.Scanner`](http://docs.oracle.com/javase/8/docs/api/java/util/Scanner.html) has a feature-rich API, it only allows parsing primitives. 
It is also [notoriously slow](https://www.cpe.ku.ac.th/~jim/java-io.html) since it uses regexes and does un-Scala things like returns nulls and throws exceptions.

`better-files` provides a faster, richer, safer, more idiomatic and compossible [Scala replacement](http://pathikrit.github.io/better-files/latest/api/#better.files.Scanner) 
that [does not use regexes](core/src/main/scala/better/files/Scanner.scala), allows peeking, returns `Option`s whenever possible and lets the user mixin custom parsers:
```scala
val data = (home / "Desktop" / "stocks.tsv") << s"""
| id  Stock Price   Buy
| ---------------------
| 1   AAPL  109.16  false
| 2   GOOGL 566.78  false
| 3   MSFT   39.10  true
""".stripMargin

val scanner: Scanner = data.newScanner().skipLines(lines = 2)

assert(scanner.peekLine == Some(" 1   AAPL  109.16  false"))
assert(scanner.peekToken == Some("1"))
assert(scanner.next(pattern = "\\d+") == Some("1"))
assert(scanner.peek[String] == Some("AAPL"))
assert(scanner.next[String]() == Some("AAPL"))
assert(scanner.next[Int]() == None)
assert(scanner.peek[Double] == Some(109.16))
assert(scanner.next[Double]() == Some(109.16))
assert(scanner.next[Boolean]() == Some(false))
assert(scanner.skip(pattern = "\\d+").next[String]() == Some("GOOGL"))

scanner.skipLine()
while(scanner.hasNext) {
  println((scanner.next[Int](), scanner.next[String](), scanner.next[Double](), scanner.next[Boolean]()))
}
```
Generic scanning:
```scala
scanner.nextDefined[A](f: String => Option[A])  // returns Some(a) if f(next) == Some(a)
scanner.nextMatch(f: String => Boolean)         // returns Some(next) if f(next) is true
scanner.nextSuccess[A](f: String => Try[A])     // returns Some(a) if f(next) == Success(a)
scanner.nextTry[A](f: String => A)              // equivalent to nextSuccess(Try(f))
```
You can also use the `peek` equivalents of above to create custom scanners:
```scala
sealed trait Animal
case class Dog(name: String) extends Animal
case class Cat(name: String) extends Animal

implicit val animalParser: Scannable[Animal] = new Scannable[Animal] {
  override def scan(token: String)(implicit context: Scanner) = for {
    name <- context.peek[String]
  } yield if (name == "Garfield") Cat(name) else Dog(name)
}

val pets = file.newScanner().iterator[Animal]
```

### File Monitoring
Vanilla Java watchers:
```scala
import java.nio.file.{StandardWatchEventKinds => EventType}
val service: java.nio.file.WatchService = myDir.newWatchService
val watcher: java.nio.file.WatchKey = myDir.newWatchKey(EventType.ENTRY_CREATE, EventType.ENTRY_DELETE)
```
The above APIs are [cumbersome to use](https://docs.oracle.com/javase/tutorial/essential/io/notification.html#process) (involves a lot of type-casting and null-checking),
are based on a blocking [polling-based model](http://docs.oracle.com/javase/8/docs/api/java/nio/file/WatchKey.html),
does not easily allow [recursive watching of directories](https://docs.oracle.com/javase/tutorial/displayCode.html?code=https://docs.oracle.com/javase/tutorial/essential/io/examples/WatchDir.java)
and nor does it easily allow [watching regular files](http://stackoverflow.com/questions/16251273/) without writing a lot of Java boilerplate.

`better-files` abstracts all the above ugliness behind a [simple interface](core/src/main/scala/better/files/FileMonitor.scala#L70):
```scala
val watcher = new FileMonitor(myDir, recursive = true) {
  override def onCreate(file: File) = println(s"$file got created")
  override def onModify(file: File) = println(s"$file got modified")
  override def onDelete(file: File) = println(s"$file got deleted")
}
watcher.start()
```
Sometimes, instead of overwriting each of the 3 methods above, it is more convenient to override the dispatcher itself:
```scala
import java.nio.file.{StandardWatchEventKinds => EventType, WatchEvent}

val watcher = new FileMonitor(myDir, recursive = true) {
  override def dispatch(eventType: WatchEvent.Kind[Path], file: File) = eventType match {
    case EventType.ENTRY_CREATE => println(s"$file got created")
    case EventType.ENTRY_MODIFY => println(s"$file got modified")
    case EventType.ENTRY_DELETE => println(s"$file got deleted")
  }
}
```

### Akka File Watcher
`better-files` also provides a powerful yet concise [reactive file watcher](akka/src/main/scala/better/files/FileWatcher.scala) 
based on [Akka actors](http://doc.akka.io/docs/akka/snapshot/scala/actors.html) that supports dynamic dispatches:
 ```scala
import akka.actor.{ActorRef, ActorSystem}
import better.files.FileWatcher._

implicit val system = ActorSystem("mySystem")

val watcher: ActorRef = (home/"Downloads").newWatcher(recursive = true)

// register partial function for an event
watcher ! on(EventType.ENTRY_DELETE) {    
  case file if file.isDirectory => println(s"$file got deleted") 
}

// watch for multiple events
watcher ! when(events = EventType.ENTRY_CREATE, EventType.ENTRY_MODIFY) {   
  case (EventType.ENTRY_CREATE, file) => println(s"$file got created")
  case (EventType.ENTRY_MODIFY, file) => println(s"$file got modified")
}
```
This is available as a stand-alone module that depends on akka:
```scala
libraryDependencies += "com.github.pathikrit" %% "better-files-akka" % version
```
