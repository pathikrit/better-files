# better-files [![License][licenseImg]][licenseLink] [![CircleCI][circleCiImg]][circleCiLink] [![Codacy][codacyImg]][codacyLink] [![Gitter][gitterImg]][gitterLink]

`better-files` is a [dependency-free](build.sbt) *pragmatic* [thin Scala wrapper](core/src/main/scala/better/files/File.scala) around [Java NIO](https://docs.oracle.com/javase/tutorial/essential/io/fileio.html).

## Tutorial [![Scaladoc][scaladocImg]][scaladocLink]
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

## sbt [![UpdateImpact][updateImpactImg]][updateImpactLink] [![VersionEye][versionEyeImg]][versionEyeLink]
In your `build.sbt`, add this:
```scala
libraryDependencies += "com.github.pathikrit" %% "better-files" % version
```
To use the [Akka based file monitor](akka), also add this:
```scala
libraryDependencies ++= Seq(  
  "com.github.pathikrit"  %% "better-files-akka"  % version,
  "com.typesafe.akka"     %% "akka-actor"         % "2.3.14"
)
```
Latest `version`: [![Maven][mavenImg]][mavenLink]

Although this library is compatible with [both Scala 2.10 and 2.11](https://oss.sonatype.org/#nexus-search;quick~better-files), it needs minimum JDK 8.

## Tests [![codecov][codecovImg]][codecovLink]
* [FileSpec](core/src/test/scala/better/files/FileSpec.scala)
* [FileWatcherSpec](akka/src/test/scala/better/files/FileWatcherSpec.scala)
* [Benchmarks](benchmarks/)

[licenseImg]: https://img.shields.io/github/license/pathikrit/better-files.svg
[licenseImg2]: https://img.shields.io/:license-mit-blue.svg
[licenseLink]: LICENSE

[circleCiImg]: https://img.shields.io/circleci/project/pathikrit/better-files/master.svg
[circleCiImg2]: https://circleci.com/gh/pathikrit/better-files/tree/master.svg
[circleCiLink]: https://circleci.com/gh/pathikrit/better-files

[codecovImg]: https://img.shields.io/codecov/c/github/pathikrit/better-files/master.svg
[codecovImg2]: https://codecov.io/github/pathikrit/better-files/coverage.svg?branch=master
[codecovLink]: http://codecov.io/github/pathikrit/better-files?branch=master

[versionEyeImg2]: https://img.shields.io/versioneye/d/pathikrit/better-files.svg
[versionEyeImg]: https://www.versioneye.com/user/projects/55f5e7de3ed894001e0003b1/badge.svg
[versionEyeLink]: https://www.versioneye.com/user/projects/55f5e7de3ed894001e0003b1

[codacyImg]: https://img.shields.io/codacy/0e2aeb7949bc49e6802afcc43a7a1aa1.svg
[codacyImg2]: https://api.codacy.com/project/badge/grade/0e2aeb7949bc49e6802afcc43a7a1aa1
[codacyLink]: https://www.codacy.com/app/pathikrit/better-files/dashboard

[mavenImg]: https://img.shields.io/maven-central/v/com.github.pathikrit/better-files_2.11.svg
[mavenImg2]: https://maven-badges.herokuapp.com/maven-central/com.github.pathikrit/better-files_2.11/badge.svg
[mavenLink]: http://search.maven.org/#search%7Cga%7C1%7Cbetter-files

[gitterImg]: https://img.shields.io/gitter/room/pathikrit/better-files.svg
[gitterImg2]: https://badges.gitter.im/Join%20Chat.svg
[gitterLink]: https://gitter.im/pathikrit/better-files

[scaladocImg]: http://img.shields.io/:docs-ScalaDoc-blue.svg
[scaladocLink]: http://pathikrit.github.io/better-files/latest/api#better.files.File

[updateImpactImg]: https://app.updateimpact.com/badge/704376701047672832/root.svg?config=compile
[updateImpactLink]: https://app.updateimpact.com/latest/704376701047672832/root
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
import better.files.{File => ScalaFile, _}
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
file.appendLine().append("world")
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
"hello" `>:` file
"world" >>: file
val bytes: Array[Byte] = file.loadBytes
```
[Fluent Interface](https://en.wikipedia.org/wiki/Fluent_interface):
```scala
 (root/"tmp"/"diary.txt")
  .createIfNotExists()  
  .appendLine()
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
val printer      : java.io.PrintWriter          = file.newPrintWriter
```
The library also adds some useful [implicits](http://pathikrit.github.io/better-files/latest/api/#better.files.Implicits) to above classes e.g.:
```scala
file1.reader > file2.writer       // pipes a reader to a writer
System.in > file2.out             // pipes an inputstream to an outputstream
src.pipeTo(sink)                  // if you don't like symbols

val bytes   : Iterator[Byte]        = inputstream.bytes
val bis     : BufferedInputStream   = inputstream.buffered  
val bos     : BufferedOutputStream  = outputstream.buffered   
val reader  : InputStreamReader     = inputstream.reader
val writer  : OutputStreamWriter    = outputstream.writer
val printer : PrintWriter           = outputstream.printWriter
val br      : BufferedReader        = reader.buffered
val bw      : BufferedWriter        = writer.buffered
val mm      : MappedByteBuffer      = fileChannel.toMappedByteBuffer
```
 
### Pattern matching
Instead of `if-else`, more idiomatic powerful Scala [pattern matching](http://pathikrit.github.io/better-files/latest/api/#better.files.File$$Types$):
```scala
/**
 * @return true if file is a directory with no children or a file with no contents
 */
def isEmpty(file: File): Boolean = file match {
  case File.Type.SymbolicLink(to) => isEmpty(to)  // this must be first case statement if you want to handle symlinks specially; else will follow link
  case File.Type.Directory(files) => files.isEmpty
  case File.Type.RegularFile(content) => content.isEmpty
  case _ => file.notExists    // a file may not be one of the above e.g. UNIX pipes, sockets, devices etc
}
// or as extractors on LHS:
val File.Type.Directory(researchDocs) = home/"Downloads"/"research"
```

### Globbing
No need to port [this](http://docs.oracle.com/javase/tutorial/essential/io/find.html) to Scala:
```scala
val dir = "src"/"test"
val matches: Iterator[File] = dir.glob("**/*.{java,scala}")
// above code is equivalent to:
dir.listRecursively.filter(f => f.extension == Some(".java") || f.extension == Some(".scala")) 
```
You can even use more advanced regex syntax instead of [glob syntax](http://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob):
```scala
val matches = dir.glob("^\\w*$")(syntax = File.PathMatcherSyntax.regex)
```
For custom cases:
```scala
dir.collectChildren(_.isSymbolicLink) // collect all symlinks in a directory
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
file.{checksum, md5, sha1, sha256, sha512, digest}   // also works for directories
file.setOwner(user: String)    // chown user file
file.setGroup(group: String)   // chgrp group file
Seq(file1, file2) >: file3     // same as cat file1 file2 > file3
Seq(file1, file2) >>: file3    // same as cat file1 file2 >> file3
file.isReadLocked / file.isWriteLocked / file.isLocked
File.newTemporaryDirectory() / File.newTemporaryFile() // create temp dir/file
```

### UNIX DSL
All the above can also be expressed using [methods](http://pathikrit.github.io/better-files/latest/api/#better.files.Cmds$) reminiscent of the command line:
```scala
import better.files_, Cmds._   // must import Cmds._ to bring in these utils
pwd / cwd     // current dir
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
md5(file) / sha1(file) / sha256(file) / sha512(file)
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
file.isParentOf / file.isChildOf / file.isSiblingOf / file.siblings
```
All the above APIs let's you specify the [`LinkOption`](http://docs.oracle.com/javase/8/docs/api/java/nio/file/LinkOption.html) either directly:
```scala
file.isDirectory(LinkOption.NOFOLLOW_LINKS)
```
Or using the [`File.Links`](http://pathikrit.github.io/better-files/latest/api/#better.files.File$$Links$) helper:
```scala
file.isDirectory(File.Links.noFollow)
```

`chmod`:
```scala
import java.nio.file.attribute.PosixFilePermission
file.addPermission(PosixFilePermission.OWNER_EXECUTE)      // chmod +X file
file.removePermission(PosixFilePermission.OWNER_WRITE)     // chmod -w file
assert(file.permissionsAsString == "rw-r--r--")

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
There are also various [`Ordering[File]` included](http://pathikrit.github.io/better-files/latest/api/#better.files.File$$Order$) e.g.:
```scala
val files = myDir.list.toSeq
files.sorted(File.Order.byName) 
files.max(File.Order.bySize) 
files.min(File.Order.byDepth) 
files.max(File.Order.byModificationTime) 
files.sorted(File.Order.byDirectoriesFirst)
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

`better-files` provides a [faster](benchmarks#benchmarks), richer, safer, more idiomatic and compossible [Scala replacement](http://pathikrit.github.io/better-files/latest/api/#better.files.Scanner) 
that [does not use regexes](core/src/main/scala/better/files/Scanner.scala), allows peeking, accessing line numbers, returns `Option`s whenever possible and lets the user mixin custom parsers:
```scala
val data = t1 << s"""
  | Hello World
  | 1 true 2 3
""".stripMargin
val scanner: Scanner = data.newScanner()
assert(scanner.next[String] == "Hello")
assert(scanner.lineNumber == 1)
assert(scanner.next[String] == "World")
assert(scanner.next[(Int, Boolean)] == (1, true))
assert(scanner.tillEndOfLine() == " 2 3")
assert(!scanner.hasNext)
```
If you are simply interested in tokens, you can use `file.tokens()`

Writing your own custom scanners:
```scala
sealed trait Animal
case class Dog(name: String) extends Animal
case class Cat(name: String) extends Animal

implicit val animalParser: Scannable[Animal] = Scannable {scanner =>
  val name = scanner.next[String]
  if (name == "Garfield") Cat(name) else Dog(name)
}

val scanner = file.newScanner()
println(scanner.next[Animal])
```

### File Monitoring
Vanilla Java watchers:
```scala
import java.nio.file.{StandardWatchEventKinds => EventType}
val service: java.nio.file.WatchService = myDir.newWatchService
myDir.register(service, events = Seq(EventType.ENTRY_CREATE, EventType.ENTRY_DELETE))
```
The above APIs are [cumbersome to use](https://docs.oracle.com/javase/tutorial/essential/io/notification.html#process) (involves a lot of type-casting and null-checking),
are based on a blocking [polling-based model](http://docs.oracle.com/javase/8/docs/api/java/nio/file/WatchKey.html),
does not easily allow [recursive watching of directories](https://docs.oracle.com/javase/tutorial/displayCode.html?code=https://docs.oracle.com/javase/tutorial/essential/io/examples/WatchDir.java)
and nor does it easily allow [watching regular files](http://stackoverflow.com/questions/16251273/) without writing a lot of Java boilerplate.

`better-files` abstracts all the above ugliness behind a [simple interface](core/src/main/scala/better/files/File.scala#L600):
```scala
val watcher = new ThreadBackedFileMonitor(myDir, recursive = true) {
  override def onCreate(file: File) = println(s"$file got created")
  override def onModify(file: File) = println(s"$file got modified")
  override def onDelete(file: File) = println(s"$file got deleted")
}
watcher.start()
```
Sometimes, instead of overwriting each of the 3 methods above, it is more convenient to override the dispatcher itself:
```scala
import java.nio.file.{Path, StandardWatchEventKinds => EventType, WatchEvent}

val watcher = new ThreadBackedFileMonitor(myDir, recursive = true) {
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
import better.files._, FileWatcher._

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
