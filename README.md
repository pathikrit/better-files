# better-files [![CircleCI][circleCiImg]][circleCiLink] [![VersionEye][versionEyeImg]][versionEyeLink] [![Codacy][codacyImg]][codacyLink] [![Bintray][bintrayImg]][bintrayLink] [![Gitter][gitterImg]][gitterLink]
[circleCiImg]: https://circleci.com/gh/pathikrit/better-files.svg?style=svg
[circleCiLink]: https://circleci.com/gh/pathikrit/better-files
[versionEyeImg]: https://www.versioneye.com/user/projects/55f5e7de3ed894001e0003b1/badge.svg?style=flat
[versionEyeLink]: https://www.versioneye.com/user/projects/55f5e7de3ed894001e0003b1
[codacyImg]: https://api.codacy.com/project/badge/0e2aeb7949bc49e6802afcc43a7a1aa1
[codacyLink]: https://www.codacy.com/app/pathikrit/better-files/dashboard
[bintrayImg]: https://api.bintray.com/packages/pathikrit/maven/better-files/images/download.svg
[bintrayLink]: https://bintray.com/pathikrit/maven/better-files/_latestVersion
[gitterImg]: https://badges.gitter.im/Join%20Chat.svg
[gitterLink]: https://gitter.im/pathikrit/better-files?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge

better-files is a [dependency-free](build.sbt) idiomatic [thin Scala wrapper](src/main/scala/better/files/package.scala) around Java file APIs 
that can be **interchangeably used with Java classes** via automatic bi-directional implicit conversions from/to Java.

1. [Instantiation](#instantiation)
1. [Simple I/O](#file-readwrite)
1. [Streams and Codecs](#streams-and-codecs)
1. [Java interpolability](#java-interpolability)
1. [Pattern matching](#pattern-matching)
1. [Globbing](#globbing)
1. [File system operations](#file-system-operations)
1. [File attributes](#file-attributes)
1. [File comparison](#file-comparison)

<!--- 
1. [Zip/Unzip](#zip-apis) 
--->
 
## Instantiation 
The following are all equivalent:
```scala
import better.files._

val f = File("/User/johndoe/Documents")
val f1: File = file"/User/johndoe/Documents"
val f2: File = root/"User"/"johndoe"/"Documents"
val f3: File = home/"Documents"
val f4: File = new java.io.File("/User/johndoe/Documents")
val f5: File = "/User"/"johndoe"/"Documents"
val f6: File = "/User/johndoe/Documents".toFile
val f7: File = root/"User"/"johndoe"/"Documents"/"presentations"/`..`
```
Resources in the classpath can be accessed using resource interpolator e.g. `resource"production.config"` 

## File Read/Write
Dead simple I/O via [Java NIO](https://en.wikipedia.org/wiki/Non-blocking_I/O_(Java)):
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
val lines  : Iterator[String]          = file.readLines
val source : scala.io.BufferedSource   = file.content 
```
You can supply your own decoder too for anything that does a read/write (it assumes `scala.io.Codec.default` if you don't provide one):
```scala
import scala.io.Codec
file.contentAsString(Codec.ISO8859)
//or
import scala.io.Codec.string2codec
file.write("hello world")(codec = "US-ASCII")
 ```
 
## Java interpolability 
You can always access the Java I/O classes:
```scala
val reader       : java.io.BufferedReader = file.reader 
val outputstream : java.io.OutputStream   = file.out 
val writer       : java.io.BufferedWriter = file.writer 
val inputstream  : java.io.InputStream    = file.in
val file         : java.io.File           = file        // implicit conversion
val path         : java.nio.file.Path     = file.path
```
The library also adds some useful implicits to above classes e.g.:
```scala
file1.reader > file2.writer       // pipes a reader to a writer
System.in > file2.out             // pipes an inputstream to an outputstream
src.pipeTo(sink)                  // if you don't like symbols
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

val Directory(docs) = (home/"Downloads"/"research.zip") unzipTo (home/"Documents")
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
file1 == file2    // equivalent to file1.samePathAs(file2)
file1 === file2   // equivalent to file1.sameContentAs(file2) (works for BOTH regular-files and directories)
```
<!---
## Zip APIs
You don't have to lookup on StackOverflow "[How to zip/unzip in Java/Scala?](http://stackoverflow.com/questions/9324933/)":
```scala
val zipFile = file"path/to/research.zip"
// Unzipping:
val research: File = zipFile.unzipTo(home/"Documents"/"research")   
// Zipping:
val zipFile = File.newTempFile("research", suffix = ".zip").zip(file1, file2, file3).create()
````
With passwords:
```scala
zipFile.unzipTo(dir, password = Some("secret-sauce"))
target.zip(file1, file2).create(password = Some("secret-sauce"))
````
--->

---
For **more examples**, consult the [tests](src/test/scala/better/FilesSpec.scala).

## sbt
The library is compatible with both Scala 2.10 and 2.11. In your `build.sbt`, add this:
```scala
resolvers += Resolver.bintrayRepo("pathikrit", "maven")

libraryDependencies += "com.github.pathikrit" %% "better-files" % "2.0.0"
```

## Future work
* Zip APIs
* File watchers using Akka actors
* Classpath resource APIs
* CSV handling?
* File converters/text extractors?
* UNIX DSL e.g. `cp(file1, file2)`, `ln(src, target)` or `rm(file*)`. [Ammonite-Ops](https://lihaoyi.github.io/Ammonite/#Ammonite-Ops) already does this.
