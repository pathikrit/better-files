# better-files [![License][licenseImg]][licenseLink] [![Maven][mavenImg]][mavenLink] [![Gitter][gitterImg]][gitterLink]

`better-files` is a [dependency-free](project/Dependencies.scala) *pragmatic* [thin Scala wrapper](core/src/main/scala/better/files/File.scala) around [Java NIO](https://docs.oracle.com/javase/tutorial/essential/io/fileio.html).

## Motivation
Imagine you have to write the following method:
1) List all `.csv` files in a directory by increasing order of file size
2) Drop the first line of each file and concat the rest into a single output file
3) Split the above output file into `n` smaller files without breaking up the lines in the input files
4) `gzip` each of the smaller output files
Note: Your program should work when files are much bigger than memory in your JVM and must close all open resources correctly

The above task is not that easy to write in Java or shell or Python without a certain amount of Googling.
Using better-files, the above problem can be solved in a fairly straightforward way:
```scala
import better.files._

def run(inputDir: File, outputDir: File, n: Int) = {
  val count = new AtomicInteger()
  val outputs = Vector.tabulate(n)(i => outputDir / s"part-$i.csv.gz")
  for {
    writers <- outputs.map(_.newGzipOutputStream().printWriter()).autoClosed
    inputFile <- inputDir.list(_.extension == Some(".csv")).toSeq.sorted(File.Order.bySize)
    line <- inputFile.lineIterator.drop(1)
  } writers(count.incrementAndGet() % n).println(line)
}
```

## Talks
  - [ScalaDays NYC 2016][scalaDaysNyc2016Event] ([slides][scalaDaysNyc2016Slides])

  <a href="http://www.youtube.com/watch?feature=player_embedded&v=uaYKkpqs6CE" target="_blank">
    <img src="site/tech_talk_preview.png" alt="ScalaDays NYC 2016: Introduction to better-files" width="480" height="360" border="10" />
  </a>

  - [ScalaDays Berlin 2016][scalaDaysBerlin2016Event] ([video][scalaDaysBerlin2016Video], [slides][scalaDaysBerlin2016Slides])
  - [Scalæ by the Bay 2016][scalæByTheBay2016Event] ([video][scalæByTheBay2016Video], [slides][scalæByTheBay2016Slides])

## Tutorial [![Scaladoc][scaladocImg]][scaladocLink]
  0. [Instantiation](#instantiation)
  0. [Simple I/O](#file-readwrite)
  0. [Resource APIs](#resource-apis)
  0. [Streams](#streams)
  0. [Encodings](#encodings)
  0. [Java serialization utils](#java-serialization-utils)
  0. [Java compatibility](#java-interoperability)
  0. [Globbing](#globbing)
  0. [File system operations](#file-system-operations)
  0. [Temporary files](#temporary-files)
  0. [UNIX DSL](#unix-dsl)
  0. [File attributes](#file-attributes)
  0. [File comparison](#file-comparison)
  0. [Zip/GZip](#zip-apis)
  0. [Automatic Resource Management](#lightweight-arm)
  0. [Scanner](#scanner)
  0. [File Monitoring](#file-monitoring)
  0. [Reactive File Watcher](#akka-file-watcher)

## sbt [![UpdateImpact][updateImpactImg]][updateImpactLink]
In your `build.sbt`, add this:
```scala
libraryDependencies += "com.github.pathikrit" %% "better-files" % version
```
To use the [Akka based file monitor](akka), also add this:
```scala
libraryDependencies ++= Seq(
  "com.github.pathikrit"  %% "better-files-akka"  % version,
  "com.typesafe.akka"     %% "akka-actor"         % "2.5.13"
)
```
Latest `version`: [![Scaladex][scaladexImg]][scaladexLink]

Although this library is currently only actively developed for Scala 2.11, 2.12 and 2.13,
you can find reasonably recent versions of this library for Scala 2.10 [here](https://oss.sonatype.org/#nexus-search;quick~better-files).

## Tests [![TravisCI][travisCiImg]][travisCiLink]  [![codecov][codecovImg]][codecovLink] [![Codacy][codacyImg2]][codacyLink] [![Known Vulnerabilities][snykImg]][snykLink] 
* [FileSpec](core/src/test/scala/better/files/FileSpec.scala)
* [FileWatcherSpec](akka/src/test/scala/better/files/FileWatcherSpec.scala)
* [Benchmarks](#benchmarks)

[licenseImg]: https://img.shields.io/github/license/pathikrit/better-files.svg
[licenseImg2]: https://img.shields.io/:license-mit-blue.svg
[licenseLink]: LICENSE

[travisCiImg]: https://img.shields.io/travis/pathikrit/better-files/master.svg
[travisCiImg2]: https://travis-ci.org/pathikrit/better-files.svg?branch=master
[travisCiLink]: https://travis-ci.org/pathikrit/better-files

[codecovImg]: https://img.shields.io/codecov/c/github/pathikrit/better-files/master.svg
[codecovImg2]: https://codecov.io/github/pathikrit/better-files/coverage.svg?branch=master
[codecovLink]: http://codecov.io/github/pathikrit/better-files?branch=master

[codacyImg]: https://img.shields.io/codacy/0e2aeb7949bc49e6802afcc43a7a1aa1.svg
[codacyImg2]: https://api.codacy.com/project/badge/grade/0e2aeb7949bc49e6802afcc43a7a1aa1
[codacyLink]: https://www.codacy.com/app/pathikrit/better-files/dashboard

[mavenImg]: https://img.shields.io/maven-central/v/com.github.pathikrit/better-files_2.12.svg
[mavenImg2]: https://maven-badges.herokuapp.com/maven-central/com.github.pathikrit/better-files_2.12/badge.svg
[mavenLink]: http://search.maven.org/#search%7Cga%7C1%7Cbetter-files

[gitterImg]: https://img.shields.io/gitter/room/pathikrit/better-files.svg
[gitterImg2]: https://badges.gitter.im/Join%20Chat.svg
[gitterLink]: https://gitter.im/pathikrit/better-files

[scaladexImg]: https://index.scala-lang.org/pathikrit/better-files/better-files/latest.svg
[scaladexLink]: https://index.scala-lang.org/pathikrit/better-files

[scaladocImg]: https://www.javadoc.io/badge/com.github.pathikrit/better-files_2.12.svg?color=blue&label=scaladocs
<!--[scaladocLink]: https://www.javadoc.io/page/com.github.pathikrit/better-files_2.12/latest/better/files/File.html-->
[scaladocLink]: http://pathikrit.github.io/better-files/latest/api/better/files/File.html

[updateImpactImg]: https://app.updateimpact.com/badge/704376701047672832/root.svg?config=compile
[updateImpactLink]: https://app.updateimpact.com/latest/704376701047672832/root

[scalaDaysNyc2016Event]: http://event.scaladays.org/scaladays-nyc-2016/#!#schedulePopupExtras-7664
[scalaDaysNyc2016Video]: https://www.youtube.com/watch?v=uaYKkpqs6CE
<!--[scalaDaysNyc2016VideoPreview]: http://img.youtube.com/vi/uaYKkpqs6CE/0.jpg-->
[scalaDaysNyc2016VideoPreview]: site/tech_talk_preview.png
[scalaDaysNyc2016Slides]: https://slides.com/pathikrit/better-files/

[scalaDaysBerlin2016Event]: http://event.scaladays.org/scaladays-berlin-2016#!#schedulePopupExtras-7668
[scalaDaysBerlin2016Video]: https://www.youtube.com/watch?v=m2YsD5cgnzI
[scalaDaysBerlin2016Slides]: https://slides.com/pathikrit/better-files/

[scalæByTheBay2016Event]: http://sched.co/7iUn
[scalæByTheBay2016Video]: https://www.youtube.com/watch?v=bLiCE6NGjrk&t=251s
[scalæByTheBay2016Slides]: https://slides.com/pathikrit/better-files/

[snykImg]: https://snyk.io/test/github/pathikrit/better-files/badge.svg?targetFile=build.sbt
[snykLink]: https://snyk.io/test/github/pathikrit/better-files?targetFile=build.sbt

-------
### Instantiation
The following are all equivalent:
```scala
import better.files._
import File._
import java.io.{File => JFile}

val f = File("/User/johndoe/Documents")                      // using constructor
val f1: File = file"/User/johndoe/Documents"                 // using string interpolator
val f2: File = "/User/johndoe/Documents".toFile              // convert a string path to a file
val f3: File = new JFile("/User/johndoe/Documents").toScala  // convert a Java file to Scala
val f4: File = root/"User"/"johndoe"/"Documents"             // using root helper to start from root
val f5: File = `~` / "Documents"                             // also equivalent to `home / "Documents"`
val f6: File = "/User"/"johndoe"/"Documents"                 // using file separator DSL
val f7: File = "/User"/'johndoe/'Documents                   // same as above but using Symbols instead of Strings
val f8: File = home/"Documents"/"presentations"/`..`         // use `..` to navigate up to parent
```

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
import better.files.Dsl.SymbolicOperations

file < "hello"     // same as file.overwrite("hello")
file << "world"    // same as file.appendLines("world")
assert(file! == "hello\nworld")
```
Or even, right-associatively:
```scala
import better.files.Dsl.SymbolicOperations

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
  .moveToDirectory(home/"Documents")
  .renameTo("princess_diary.txt")
  .changeExtensionTo(".md")
  .lines
```

### Resource APIs
Confused by the [various ways to load resources in Java](https://stackoverflow.com/questions/3861989/preferred-way-of-loading-resources-in-java)?
Worry no more:
```scala
val resource        : InputStream   = Resource.getAsStream("foo.txt") //Same as this.getClass().getResource("foo.txt")
val resourceURL     : java.net.URL  = Resource.getUrl("foo.txt")
val rootResourceURL : java.net.URL  = Resource.getUrl()
val resourceAsStr   : String        = Resource.getAsString("foo.txt")
```
The above APIs can load from custom `ClassLoader`s too:
```scala
val resource        : InputStream   = Resource.at[MyClass].getAsStream("foo.txt")
``` 

### Streams
Various ways to slurp a file without loading its contents into memory:
 ```scala
val bytes  : Iterator[Byte]            = file.bytes
val chars  : Iterator[Char]            = file.chars
val lines  : Iterator[String]          = file.lineIterator      //file.lines loads all lines in memory
```
Note: The above APIs can be traversed at most once e.g. `file.bytes` is a `Iterator[Byte]` which only allows `TraversableOnce`.
To traverse it multiple times without creating a new iterator instance, convert it into some other collection e.g. `file.bytes.toStream`

You can write an `Iterator[Byte]` or an `Iterator[String]` back to a file:
```scala
file.writeBytes(bytes)
file.printLines(lines)
```

[`tee`](http://stackoverflow.com/questions/7987395/) multiple outputstreams:
```scala
val s3 = s1 tee s2
s3.printWriter.println(s"Hello world") // gets written to both s1 and s2
```

### Encodings
You can supply your own charset too for anything that does a read/write (it assumes `java.nio.charset.Charset.defaultCharset()` if you don't provide one):
```scala
val content: String = file.contentAsString  // default charset

// custom charset:
import java.nio.charset.Charset
file.contentAsString(charset = Charset.forName("US-ASCII"))

//or simply using implicit conversion from Strings
file.write("hello world")(charset = "US-ASCII")
 ```

Note: By default, `better-files` [correctly handles BOMs while decoding](core/src/main/scala/better/files/UnicodeCharset.scala).
If you wish to have the [incorrect JDK behaviour](http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4508058),
you would need to supply Java's UTF-8 charset e.g.:
```scala
file.contentAsString(charset = Charset.forName("UTF-8"))    // Default incorrect JDK behaviour for UTF-8 (see: JDK-4508058)
```

If you also wish to write BOMs while encoding, you would need to supply it as:
```scala
file.write("hello world")(charset = UnicodeCharset("UTF-8", writeByteOrderMarkers = true))
```

### Java serialization utils
Some common utils to serialize/deserialize using Java's serialization util
```scala
case class Person(name: String, age: Int)
val person = new Person("Chris", 24)

// Write
file.newOutputStream.asObjectOutputStream.serialize(obj).flush()

// Read
val person2 = file.newInputStream.asObjectInputStream.deserialize[Person]
assert(person == person2)

// Read using custom class loader:
file.newInputStream.asObjectInputStreamUsingClassLoader(classLoader = myClassLoader).deserialize[Person]
```

The above can be simply written as:
```scala
val person2: Person = file.writeSerialized(person).readDeserialized[Person]()
assert(person == person2)
```

### Java interoperability
You can always access the Java I/O classes:
```scala
val file: File = tmp / "hello.txt"
val javaFile     : java.io.File                 = file.toJava
val uri          : java.net.URI                 = file.uri
val url          : java.net.URL                 = file.url
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
The library also adds some useful [implicits](http://pathikrit.github.io/better-files/latest/api/better/files/Implicits.html) to above classes e.g.:
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
val str     : String                = inputstream.asString  //Read a string from an InputStream
val in      : InputStream           = str.inputStream
val reader  : Reader                = str.reader
val lines   : Seq[String]           = str.lines
```
`better-files` also supports [certain conversions that are not supported out of the box by the JDK](https://stackoverflow.com/questions/62241/how-to-convert-a-reader-to-inputstream-and-a-writer-to-outputstream)

### Globbing
No need to port [this](http://docs.oracle.com/javase/tutorial/essential/io/find.html) to Scala:
```scala
val dir = "src"/"test"
val matches: Iterator[File] = dir.glob("*.{java,scala}")
// above code is equivalent to:
dir.listRecursively.filter(f => f.extension == Some(".java") || f.extension == Some(".scala"))
```

You can even use more advanced regex syntax instead of [glob syntax](http://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob):
```scala
val matches = dir.globRegex("^\\w*$".r) //equivalent to dir.glob("^\\w*$")(syntax = File.PathMatcherSyntax.regex)
```

By default, glob syntax in `better-files` is [different from](https://github.com/pathikrit/better-files/issues/114)
the default JDK glob behaviour since it always includes path. To use the default behaviour:
```scala
dir.glob("**/*.txt", includePath = false) // JDK default
//OR
dir.glob("*.txt", includePath = true) // better-files default
```
You can also extend the `File.PathMatcherSyntax` to create your own matchers.

For custom cases:
```scala
dir.collectChildren(_.isSymbolicLink) // collect all symlinks in a directory
```
For simpler cases, you can always use `dir.list` or `dir.walk(maxDepth: Int)`

### File system operations
Utilities to `ls`, `cp`, `rm`, `mv`, `ln`, `md5`, `touch`, `cat` etc:
```scala
file.touch()
file.delete()     // unlike the Java API, also works on directories as expected (deletes children recursively)
file.clear()      // If directory, deletes all children; if file clears contents
file.renameTo(newName: String)
file.moveTo(destination)
file.moveToDirectory(destination)
file.copyTo(destination)       // unlike the default API, also works on directories (copies recursively)
file.copyToDirectory(destination)
file.linkTo(destination)                     // ln destination file
file.symbolicLinkTo(destination)             // ln -s destination file
file.{checksum, md5, sha1, sha256, sha512, digest}   // also works for directories
file.setOwner(user: String)      // chown user file
file.setGroup(group: String)     // chgrp group file
Seq(file1, file2) `>:` file3     // same as cat file1 file2 > file3 (must import import better.files.Dsl.SymbolicOperations)
Seq(file1, file2) >>: file3      // same as cat file1 file2 >> file3 (must import import better.files.Dsl.SymbolicOperations)
file.isReadLocked; file.isWriteLocked; file.isLocked
File.numberOfOpenFileDescriptors        // number of open file descriptors
```

### Temporary files
Utils to create temporary files:
```scala
File.newTemporaryDirectory()
File.newTemporaryFile()
```
The above APIs allow optional specifications of `prefix`, `suffix` and `parentDir`.
These files are [not deleted automatically on exit by the JVM](http://stackoverflow.com/questions/16691437/when-are-java-temporary-files-deleted) (you have to set `deleteOnExit` which adds to `shutdownHook`).

A cleaner alternative is to use self-deleting file contexts which deletes the file immediately when done:
```scala
for {
 tempFile <- File.temporaryFile()
} doSomething(tempFile) // tempFile is auto deleted at the end of this block - even if an exception happens
```

OR equivalently:
```scala
File.usingTemporaryFile() {tempFile =>
  //do something
}  // tempFile is auto deleted at the end of this block - even if an exception happens
```

You can make any files temporary (i.e. delete after use) by doing this:
```scala
val foo = File.home / "Downloads" / "foo.txt"

for {
 temp <- foo.toTemporary
} doSomething(temp) // foo is deleted at the end of this block - even if an exception happens
```

### UNIX DSL
All the above can also be expressed using [methods](http://pathikrit.github.io/better-files/latest/api/better/files/Dsl$.html) reminiscent of the command line:
```scala
import better.files._
import better.files.Dsl._   // must import Dsl._ to bring in these utils

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
md5(file); sha1(file); sha256(file); sha512(file)
unzip(zipFile)(targetDir)
zip(file*)(targetZipFile)
ungzip(gzipFile)(targetFile)
gzip(file)(targetGZipFile)
```

### File attributes
Query various file attributes e.g.:
```scala
file.name       // simpler than java.io.File#getName
file.extension
file.contentType
file.lastModifiedTime     // returns JSR-310 time
file.owner
file.group
file.isDirectory; file.isSymbolicLink; file.isRegularFile
file.isHidden
file.hide(); file.unhide()
file.isOwnerExecutable; file.isGroupReadable // etc. see file.permissions
file.size                 // for a directory, computes the directory size
file.posixAttributes; file.dosAttributes  // see file.attributes
file.isEmpty      // true if file has no content (or no children if directory) or does not exist
file.isParentOf; file.isChildOf; file.isSiblingOf; file.siblings
file("dos:system") = true  // set custom meta-data for file (similar to Files.setAttribute)
```
All the above APIs let you specify the [`LinkOption`](http://docs.oracle.com/javase/8/docs/api/java/nio/file/LinkOption.html) either directly:
```scala
file.isDirectory(LinkOption.NOFOLLOW_LINKS)
```
Or using the [`File.LinkOptions`](http://pathikrit.github.io/better-files/latest/api/better/files/File$$LinkOptions$.html) helper:
```scala
file.isDirectory(File.LinkOptions.noFollow)
```

`chmod`:
```scala
import java.nio.file.attribute.PosixFilePermission
file.addPermission(PosixFilePermission.OWNER_EXECUTE)      // chmod +X file
file.removePermission(PosixFilePermission.OWNER_WRITE)     // chmod -w file
assert(file.permissionsAsString == "rw-r--r--")

// The following are all equivalent:
assert(file.permissions contains PosixFilePermission.OWNER_EXECUTE)
assert(file.testPermission(PosixFilePermission.OWNER_EXECUTE))
assert(file.isOwnerExecutable)
```

### File comparison
Use `==` to check for path-based equality and `===` for content-based equality:
```scala
file1 == file2    // equivalent to `file1.isSamePathAs(file2)`
file1 === file2   // equivalent to `file1.isSameContentAs(file2)` (works for regular-files and directories)
file1 != file2    // equivalent to `!file1.isSamePathAs(file2)`
file1 !== file2   // equivalent to `!file1.isSameContentAs(file2)`
```
There are also various [`Ordering[File]` instances](http://pathikrit.github.io/better-files/latest/api/better/files/File$$Order$.html) included, e.g.:
```scala
val files = myDir.list.toSeq
files.sorted(File.Order.byName)
files.max(File.Order.bySize)
files.min(File.Order.byDepth)
files.max(File.Order.byModificationTime)
files.sorted(File.Order.byDirectoriesFirst)
```

### Zip APIs
You don't have to lookup on StackOverflow "[How to zip/unzip/gzip in Java/Scala?](http://stackoverflow.com/questions/9324933/)":
```scala
// Unzipping:
val zipFile: File = file"path/to/research.zip"
val research: File = zipFile.unzipTo(destination = home/"Documents"/"research")

// Zipping:
val zipFile: File = directory.zipTo(destination = home/"Desktop"/"toEmail.zip")

// Zipping in:
val zipFile = File("countries.zip").zipIn(Iterator(file"usa.txt", file"russia.txt"))()

// Zipping/Unzipping to temporary files/directories:
val someTempZipFile: File = directory.zip()
val someTempDir: File = someTempZipFile.unzip()
assert(directory === someTempDir)
```

Mapping over each `ZipEntry`:
```scala
val fileNames = zipFile.newZipInputStream.mapEntries(_.getName) // gets the file names inside the zip file
```

GZIP handling:
```scala
File("big-data.csv").gzipTo(File("big-data.csv.gz"))
File("big-data.csv.gz").unGzipTo(File("big-data.csv"))

// GZIP stream handling:
File("countries.gz").newInputStream.asGzipInputStream().lines.take(10).foreach(println)

def write(out: OutputStream, countries: Seq[String]) =
  out.asGzipOutputStream().printWriter().printLines(countries).close()
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
 reader <- file.bufferedReader    // returns Dispose[BufferedReader]
} foo(reader)

// or simply:
file.bufferedReader.foreach(foo)
```

Similarly:
```scala
for {
 reader <- file.bufferedReader
} yield foo(reader)

// Simpler
file.bufferedReader.map(foo).get()

// Even simpler
file.bufferedReader.apply(foo)
```

If `foo` itself is lazy and depends on `reader` being open, you should `flatMap` instead of `apply`:
```scala
def lines(reader: BufferedReader): Iterator[String] = ???

for {
  reader <- file.bufferedReader
  line <- lines(reader)
} yield line

// or simply
file.bufferedReader.flatMap(lines)
```

You can also define your own custom disposable resources e.g.:
```scala
trait Shutdownable {
  def shutdown(): Unit = ()
}

object Shutdownable {
  implicit val disposable: Disposable[Shutdownable] = Disposable(_.shutdown())
}

val s: Shutdownable = ....

for {
  instance <- new Dispose(s)
} doSomething(s)  // s is disposed after this
```

`using` syntax:
```scala
val lines: List[String] = using(file.newInputStream) { stream =>
  stream.lines.toList   // Must be eager so .toList
}
```

### Scanner
Although [`java.util.Scanner`](http://docs.oracle.com/javase/8/docs/api/java/util/Scanner.html) has a feature-rich API, it only allows parsing primitives.
It is also [notoriously slow](https://www.cpe.ku.ac.th/~jim/java-io.html) since it uses regexes and does un-Scala things like returns nulls and throws exceptions.

`better-files` provides a [faster](#benchmarks), richer, safer, more idiomatic and compossible [Scala replacement](http://pathikrit.github.io/better-files/latest/api/better/files/Scanner.html)
that [does not use regexes](core/src/main/scala/better/files/Scanner.scala), allows peeking, accessing line numbers, returns `Option`s whenever possible and lets the user mixin custom parsers:
```scala
val f1 = File("/tmp/temp.txt")
val data = f1.overwrite(s"""Hello World
  | 1 true
  | 2 3
""".stripMargin)
val scanner: Scanner = data.newScanner()
assert(scanner.next[String] == "Hello")
assert(scanner.lineNumber == 1)
assert(scanner.next[String] == "World")
assert(scanner.next[(Int, Boolean)] == (1, true))
assert(scanner.nextLine() == " 2 3")
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

The [shapeless-scanner](core/src/test/scala/better/files/ShapelessScannerSpec.scala) lets you scan [`HList`s](https://github.com/milessabin/shapeless/blob/master/core/src/main/scala/shapeless/hlists.scala):
```scala
val in = Scanner("""
  12 Bob True
  13 Mary False
  26 Rick True
""")

import shapeless._

type Row = Int :: String :: Boolean :: HNil

val out = Seq.fill(3)(in.next[Row])
assert(out == Seq(
  12 :: "Bob" :: true :: HNil,
  13 :: "Mary" :: false :: HNil,
  26 :: "Rick" :: true :: HNil
))
```

[and case-classes](https://meta.plasm.us/posts/2015/11/08/type-classes-and-generic-derivation/):

```scala
case class Person(id: Int, name: String, isMale: Boolean)
val out2 = Seq.fill(3)(in.next[Person])
```

Simple CSV reader:
```scala
val file = """
  23,foo
  42,bar
"""
val csvScanner = file.newScanner(StringSplitter.on(','))
csvScanner.next[Int]    //23
csvScanner.next[String] //foo
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

`better-files` abstracts all the above ugliness behind a [simple interface](core/src/main/scala/better/files/File.scala#1100):
```scala
val watcher = new FileMonitor(myDir, recursive = true) {
  override def onCreate(file: File, count: Int) = println(s"$file got created")
  override def onModify(file: File, count: Int) = println(s"$file got modified $count times")
  override def onDelete(file: File, count: Int) = println(s"$file got deleted")
}
watcher.start() 
Thread.sleep(60 * 1000) // The above line starts the monitoring asynchronously 
```
Sometimes, instead of overwriting each of the 3 methods above, it is more convenient to override the dispatcher itself:
```scala
import java.nio.file.{Path, StandardWatchEventKinds => EventType, WatchEvent}

val watcher = new FileMonitor(myDir, recursive = true) {
  override def onEvent(eventType: WatchEvent.Kind[Path], file: File, count: Int) = eventType match {
    case EventType.ENTRY_CREATE => println(s"$file got created")
    case EventType.ENTRY_MODIFY => println(s"$file got modified $count")
    case EventType.ENTRY_DELETE => println(s"$file got deleted")
  }
}
```

There is also an external module which gives high performance file monitoring and interpolates with better-files.
See: https://github.com/gmethvin/directory-watcher#better-files-integration-scala

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
  case (EventType.ENTRY_CREATE, file, count) => println(s"$file got created")
  case (EventType.ENTRY_MODIFY, file, count) => println(s"$file got modified $count times")
}
```

## Benchmarks
* [Scanner benchmarks](core/src/test/scala/better/files/benchmarks/Scanners.scala):
```shell
> sbt "core/testOnly better.files.benchmarks.*"
JavaScanner              : 2191 ms
StringBuilderScanner     : 1325 ms
CharBufferScanner        : 1117 ms
StreamingScanner         :  212 ms
IterableScanner          :  365 ms
IteratorScanner          :  297 ms
BetterFilesScanner       :  272 ms
ArrayBufferScanner       :  220 ms
FastJavaIOScanner2       :  181 ms
FastJavaIOScanner        :  179 ms
```

----

[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/)

YourKit supports better-files with its full-featured Java Profiler.
YourKit, LLC is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/) and [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/),
innovative and intelligent tools for profiling Java and .NET applications.

