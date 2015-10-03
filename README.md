# better-files [![CircleCI][circleCiImg]][circleCiLink] [![Codacy][codacyImg]][codacyLink] [![Gitter][gitterImg]][gitterLink]

`better-files` is a [dependency-free](build.sbt) *pragmatic* [thin Scala wrapper](src/main/scala/better/files/package.scala) around [Java NIO](https://docs.oracle.com/javase/tutorial/essential/io/fileio.html)

## Tutorial
  * [Instantiation](#instantiation)
  * [Simple I/O](#file-readwrite)
  * [Streams and Codecs](#streams-and-codecs)
  * [Java compatibility](#java-interoperability)
  * [Pattern matching](#pattern-matching)
  * [Globbing](#globbing)
  * [File system operations](#file-system-operations)
  * [UNIX DSL](#unix-dsl)
  * [File attributes](#file-attributes)
  * [File comparison](#file-comparison)
  * [Zip/Unzip](#zip-apis)
  * [Automatic Resource Management](#lightweight-arm)
  * [Scanner] (#scanner)
  * [File Watchers](#file-watchers)

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

**Note**: Rename the import if you think the usage of the word `File` may confuse your teammates:
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
val source : scala.io.BufferedSource   = file.content 
val buffer : java.nio.ByteBuffer       = file.byteBuffer
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
val reader       : java.io.BufferedReader       = file.reader 
val outputstream : java.io.OutputStream         = file.out 
val writer       : java.io.BufferedWriter       = file.writer 
val inputstream  : java.io.InputStream          = file.in
val path         : java.nio.file.Path           = file.path
val fs           : java.nio.file.FileSystem     = file.fileSystem
val channel      : java.nio.channel.FileChannel = file.channel
val ram          : java.io.RandomAccessFile     = file.randomAccess
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
val br      : BufferedReader        = reader.buffered
val bw      : BufferedWriter        = writer.buffered
```
 
### Pattern matching
Instead of `if-else`, more idiomatic powerful Scala pattern matching:
```scala
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
file.renameTo(newName: String)
file.moveTo(destination)
file.copyTo(destination)
file.linkTo(destination)                     // ln file destination
file.symLinkTo(destination)                  // ln -s file destination
file.{checksum, md5, digest}   // also works for directories; used for fast equality of directories
file.setOwner(user: String)    // chown user file
file.setGroup(group: String)   // chgrp group file
Seq(file1, file2) >: file3     // same as cat file1 file2 > file3
Seq(file1, file2) >>: file3    // same as cat file1 file2 >> file3
```

### UNIX DSL
All the above can also be expressed using methods reminiscent of the command line:
```scala
import better.files_, Cmds._   // must import Cmds._ to bring in these utils
cp(file1, file2)
mv(file1, file2)
rm(file) / del(file)
ls(file) / dir(file)
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
file1 == file2    // equivalent to `file1.samePathAs(file2)`
file1 === file2   // equivalent to `file1.sameContentAs(file2)` (works for regular-files and directories)
file1 != file2    // equivalent to `!file1.samePathAs(file2)`
file1 =!= file2   // equivalent to `!file1.sameContentAs(file2)`
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
File("countries.gz").in.gzipped.lines.take(10).foreach(println)
```

### Lightweight ARM
Auto-close Java closeables (see [scala-arm](https://github.com/jsuereth/scala-arm/)):
```scala
import better.files._, Closeable.managed
for {
  in <- managed(file1.newInputStream)
  out <- managed(file2.newOutputStream)
} in.pipeTo(out)
//No need to close them after the for-each
```

### Scanner
Although [`java.util.Scanner`](http://docs.oracle.com/javase/8/docs/api/java/util/Scanner.html) has a feature-rich API,
it is [notoriously slow](https://www.cpe.ku.ac.th/~jim/java-io.html) since it uses regexes and does un-Scala things like returns nulls and throws exceptions.

`better-files` provides a faster, richer, safer and more idiomatic [Scala replacement](http://pathikrit.github.io/better-files/latest/api/#better.files.Scanner) 
that [does not use regexes](src/main/scala/better/files/Scanner.scala), allows peeking and returns `Option`s whenever possible:
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
scanner.next[A](f: String => Option[A])       // returns Some(a) if f(next) == Some(a)
scanner.nextMatch(f: String => Boolean)       // returns Some(next) if f(next) is true
scanner.nextSuccess[A](f: String => Try[A])   // returns Some(a) if f(next) == Success(a)
scanner.nextTry[A](f: String => A)            // equivalent to nextSuccess(Try(f))
```
Custom scanners:
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

### File Watchers (WIP)
Vanilla Java watchers:
```scala
import java.nio.file.{StandardWatchEventKinds => WatchEvents}
val service: java.nio.file.WatchService = file.newWatchService
val watcher: java.nio.file.WatchKey = file.newWatchKey(WatchEvents.ENTRY_CREATE, WatchEvents.ENTRY_DELETE)
```
The above APIs are [cumbersome to use](https://docs.oracle.com/javase/tutorial/essential/io/notification.html#process) (involves a lot of type-casting and null-checking),
are based on a blocking [polling-based model](http://docs.oracle.com/javase/8/docs/api/java/nio/file/WatchKey.html) 
and does not easily [allow nested directory watching](https://docs.oracle.com/javase/tutorial/essential/io/examples/WatchDir.java).

`better-files` provides a concise, type-safe and reactive file watcher API based on [Akka actors](http://doc.akka.io/docs/akka/snapshot/scala/actors.html):
 ```scala
import akka.actor.ActorSystem
implicit val actorSystem = ActorSystem("actorSystem")
val fileWatcher = dir.newWatcher(recursive = true, concurrency = 2)

fileWatcher.when(event = WatchEvents.ENTRY_CREATE, WatchEvents.MODIFY) {
  case (WatchEvents.ENTRY_CREATE, file) => println(s"Created: $file")
  case (WatchEvents.ENTRY_MODIFY, file) => println(s"Modified: $file")
}

fileWatcher.when(event = WatchEvents.ENTRY_DELETE) { (file, event) =>
  println(s"$file got deleted")
}
```
