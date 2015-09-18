# better-files [![CircleCI][circleCiImg]][circleCiLink] [![Codacy][codacyImg]][codacyLink] [![Gitter][gitterImg]][gitterLink]

[dependency-free](build.sbt) thin [Scala wrapper](src/main/scala/better/files/package.scala) around [Java NIO](https://en.wikipedia.org/wiki/Non-blocking_I/O_(Java)):

1. [Instantiation](#instantiation)
1. [Simple I/O](#file-readwrite)
1. [Streams and Codecs](#streams-and-codecs)
1. [Java compatibility](#java-interoperability)
1. [Pattern matching](#pattern-matching)
1. [Globbing](#globbing)
1. [File system operations](#file-system-operations)
1. [UNIX DSL](#unix-dsl)
1. [File attributes](#file-attributes)
1. [File comparison](#file-comparison)
1. [Zip/Unzip](#zip-apis)
 
## Instantiation 
The following are all equivalent:
```scala
import better.files._

val f = File("/User/johndoe/Documents")
val f1: File = file"/User/johndoe/Documents"
val f2: File = root/"User"/"johndoe"/"Documents"
val f3: File = home/"Documents"
val f4: File = new java.io.File("/User/johndoe/Documents").toScala
val f5: File = "/User"/"johndoe"/"Documents"
val f6: File = "/User/johndoe/Documents".toFile
val f7: File = root/"User"/"johndoe"/"Documents"/"presentations"/`..`
```
Resources in the classpath can be accessed using resource interpolator e.g. `resource"production.config"` 

## File Read/Write
Dead simple I/O:
```scala
val file = root/"tmp"/"test.txt"
file.overwrite("hello")
file.append("world")
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

## Streams and Codecs
Various ways to slurp a file without loading the contents into memory:
 ```scala
val bytes  : Iterator[Byte]            = file.bytes
val chars  : Iterator[Char]            = file.chars
val lines  : Iterator[String]          = file.lines
val source : scala.io.BufferedSource   = file.content 
```
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
 
## Java interoperability
You can always access the Java I/O classes:
```scala
val file: File = tmp / "hello.txt"
val javaFile     : java.io.File                 = file.toJava
val reader       : java.io.BufferedReader       = file.reader 
val outputstream : java.io.OutputStream         = file.out 
val writer       : java.io.BufferedWriter       = file.writer 
val inputstream  : java.io.InputStream          = file.in
val path         : java.nio.file.Path           = file.path
val fs           : java.nio.file.FileSystem     = file.fileSystem
val channel      : java.nio.channel.FileChannel = file.channel
```
The library also adds some useful implicits to above classes e.g.:
```scala
file1.reader > file2.writer       // pipes a reader to a writer
System.in > file2.out             // pipes an inputstream to an outputstream
src.pipeTo(sink)                  // if you don't like symbols

val bis     : BufferedInputStream   = inputstream.buffered  
val bos     : BufferedOutputStream  = outputstream.buffered   
val reader  : InputStreamReader     = inputstream.reader
val writer  : OutputStreamWriter    = outputstream.writer
val br      : BufferedReader        = reader.buffered
val bw      : BufferedWriter        = writer.buffered
```
 
## Pattern matching
Instead of `if-else`, more idiomatic powerful Scala pattern matching:
```scala
"src"/"test"/"foo" match {
  case SymbolicLink(to) =>          // this must be first case statement if you want to handle symlinks specially; else will follow link
  case Directory(children) =>       
  case RegularFile(source) =>       
  case other if other.exists() =>   // a file may not be one of the above e.g. UNIX pipes, sockets, devices etc
  case _ =>                         // a file that does not exist
}
// or as extractors on LHS:
val Directory(researchDocs) = home/"Downloads"/"research"
```

## Globbing
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
For simpler cases, you can always use `dir.list` or `dir.listRecursively(maxDepth: Int)`

## File system operations
Utilities to `ls`, `cp`, `rm`, `mv`, `ln`, `md5`, `diff`, `touch`, `cat` etc:
```scala
file.touch()
file.delete()     // unlike the Java API, also works on directories as expected (deletes children recursively)
file.renameTo(newName: String)
file.moveTo(destination)
file.copyTo(destination)
file.linkTo(destination)                     // ln file destination
file.symLinkTo(destination)                  // ln -s file destination
file.checksum
file.setOwner(user: String)    // chown user file
file.setGroup(group: String)   // chgrp group file
Seq(file1, file2) >: file3     // same as cat file1 file2 > file3
Seq(file1, file2) >>: file3    // same as cat file1 file2 >> file3
```

## UNIX DSL
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
chown(owner, file)
chgrp(owner, file)
chmod_+(permission, files)  // add permission
chmod_-(permission, files)  // remove permission
unzip(file)
zip(file*) >>: output
```

## File attributes
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
```
`chmod`:
```scala
import java.nio.file.attribute.PosixFilePermission._
file.addPermissions(OWNER_EXECUTE, GROUP_EXECUTE)      // chmod +X file
file.removePermissions(OWNER_WRITE)                    // chmod -w file
// The following are all equivalent:
assert(file.permissions contains OWNER_EXECUTE)
assert(file(OWNER_EXECUTE))
assert(file.isOwnerExecutable)
```

## File comparison
Use `==` to check for path-based equality and `===` for content-based equality
```scala
file1 == file2    // equivalent to `file1.samePathAs(file2)`
file1 === file2   // equivalent to `file1.sameContentAs(file2)` (works for regular-files and directories)
```

## Zip APIs (WIP)
You don't have to lookup on StackOverflow "[How to zip/unzip in Java/Scala?](http://stackoverflow.com/questions/9324933/)":
```scala
// Unzipping:
val zipFile = file"path/to/research.zip"
val research: File = zipFile unzipTo (home/"Documents"/"research")   
// Zipping:
val target = File.newTempFile("research", suffix = ".zip")
val zipFile = target.zip(file1, file2, file3).create()
````
With passwords:
```scala
zipFile.unzipTo(dir, password = Some("secret-sauce"))
target.zip(file1, file2).create(password = Some("secret-sauce"))
```

---
For **more examples**, consult the [tests](src/test/scala/better/FilesSpec.scala).

## sbt [![VersionEye][versionEyeImg]][versionEyeLink]
The library is compatible with [both Scala 2.10 and 2.11](https://bintray.com/pathikrit/maven/better-files#files). In your `build.sbt`, add this:
```scala
resolvers += Resolver.bintrayRepo("pathikrit", "maven")

libraryDependencies += "com.github.pathikrit" %% "better-files" % version
```
Latest `version`: [![Bintray][bintrayImg]][bintrayLink]

## Future work
* File watchers using Akka actors
* Classpath resource APIs
* Non-blocking APIs
* CSV handling
* File converters/text extractors

[circleCiImg]: https://img.shields.io/circleci/project/pathikrit/better-files.svg
[circleCiLink]: https://circleci.com/gh/pathikrit/better-files
[versionEyeImg]: https://www.versioneye.com/user/projects/55f5e7de3ed894001e0003b1/badge.svg?style=flat
[versionEyeLink]: https://www.versioneye.com/user/projects/55f5e7de3ed894001e0003b1
[codacyImg]: https://img.shields.io/codacy/0e2aeb7949bc49e6802afcc43a7a1aa1.svg
[codacyLink]: https://www.codacy.com/app/pathikrit/better-files/dashboard
[bintrayImg]: https://img.shields.io/bintray/v/pathikrit/maven/better-files.svg
[bintrayLink]: https://bintray.com/pathikrit/maven/better-files
[gitterImg]: https://badges.gitter.im/Join%20Chat.svg
[gitterLink]: https://gitter.im/pathikrit/better-files
