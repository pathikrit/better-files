better-files [![CircleCI][circleCiImg]][circleCiLink] [![VersionEye][versionEyeImg]][versionEyeLink] [![Codacy][codacyImg]][codacyLink] [![Bintray][bintrayImg]][bintrayLink] [![Gitter][gitterImg]][gitterLink]
---
[circleCiImg]: https://circleci.com/gh/pathikrit/better-files.svg?style=svg&circle-token=3800512b1d901f1cf24538b392df471117d40cfb
[circleCiLink]: https://circleci.com/gh/pathikrit/better-files
[versionEyeImg]: https://www.versioneye.com/user/projects/55f5e7de3ed894001e0003b1/badge.svg?style=flat
[versionEyeLink]: https://www.versioneye.com/user/projects/55f5e7de3ed894001e0003b1
[codacyImg]: https://api.codacy.com/project/badge/0e2aeb7949bc49e6802afcc43a7a1aa1
[codacyLink]: https://www.codacy.com/app/pathikrit/better-files/dashboard
[bintrayImg]: https://api.bintray.com/packages/pathikrit/maven/better-files/images/download.svg
[bintrayLink]: https://bintray.com/pathikrit/maven/better-files/_latestVersion
[gitterImg]: https://gitter.im/pathikrit/better-files
[gitterLink]: https://gitter.im/pathikrit/better-files?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge

better-files is a [dependency-free](build.sbt) idiomatic [thin Scala wrapper](src/main/scala/better/files/package.scala) around Java file APIs 
that can be **interchangeably used with Java classes** via automatic bi-directional implicit conversions from/to Java.

**Instantiation**: The following are all equivalent:
```scala
import better.files._

val f = File("/User/johndoe/Documents")
val f1: File = file"/User/johndoe/Documents"
val f2: File = root / "User" / "johndoe" / "Documents"
val f3: File = home / "Documents"
val f4: File = new java.io.File("/User/johndoe/Documents")
val f5: File = "/User" / "johndoe" / "Documents"
val f6: File = "/User/johndoe/Documents".toFile
val f7: File = root / "User" / "johndoe" / "Documents" / "presentations" / `..`
```
Resources in the classpath can be accessed using resource interpolator e.g. `resource"production.config"` 

**File I/O**: Dead simple I/O via [Java NIO](https://en.wikipedia.org/wiki/Non-blocking_I/O_(Java)):
```scala
val file = root / "tmp" / "test.txt"
file.overwrite("hello")
file.append("world")
assert(file.contents == "hello\nworld")
val contents: Array[Byte] = file.bytes
```
If you are someone who likes symbols, then the above code can also be written as:
```scala
file < "hello"
file << "world"
assert(file! == "hello\nworld")
```
Or even, right-associatively:
```scala
"hello" >: file
"world" >>: file 
```
All operations are chainable e.g.
```scala
 (root / "tmp" / "diary.txt")
  .createIfNotExists()  
  .appendNewLine
  .appendLines("My name is", "Inigo Montoya")
  .moveTo(home / "Documents")
  .renameTo("princess_diary.txt")
  .changeExtensionTo(".md")
  .readLines
```

**Powerful pattern matching**: Instead of `if-else`, more readable Scala pattern matching:
```scala
"src" / "test" / "foo" match {
  case SymbolicLink(to) =>          //this must be first case statement if you want to handle symlinks specially; else will follow link
  case Directory(children) => 
  case RegularFile(contents) => 
  case other if other.exists() =>   //A file may not be one of the above e.g. UNIX pipes, sockets, devices etc
  case _ =>                         //A file that does not exist
}
```

**Globbing**: No need to port [this](http://docs.oracle.com/javase/tutorial/essential/io/find.html) to Scala:
```scala
val dir = "src" / "test"
val matches: Seq[File] = dir.glob("**/*.{java,scala}")
// above code is equivalent to:
dir.listRecursively.filter(f => f.extension == Some(".java") || f.extension == Some(".scala")) 
```
You can even use more advanced regex syntax instead of glob syntax:
```scala
val matches = dir.glob("^\\w*$", syntax = "regex")
```
For simpler cases, you can always use `dir.list` or `dir.listRecursively(maxDepth: Int)`

**File-system operations**: Utilities to `ls`, `cp`, `rm`, `mv`, `ln`, `md5`, `diff`, `touch` etc:
```scala
file.touch()
file.delete()     // unlike the Java API, also works on directories as expected (deletes children recursively)
file.renameTo(newName: String)
file.moveTo(destination)
file.copyTo(destination)
file.linkTo(destination)                     // ln file destination
file.linkTo(destination, symbolic = true)    // ln -s file destination
file.checksum
file.setOwner(user: String)     // chown user file
file.setGroup(group: String)    // chgrp group file
```
`chmod`:
```scala
import java.nio.file.attribute.PosixFilePermission._
file.addPermissions(OWNER_EXECUTE)     // chmod +x file
file.removePermissions(OWNER_WRITE)    // chmod -w file
// The following are all equivalent:
assert(file.permissions contains OWNER_EXECUTE)
assert(file(OWNER_EXECUTE))
assert(file.isOwnerExecutable)
```

**File attribute APIs**: Query various file attributes e.g.:
```scala
file.name       // simpler than java.io.File#getName
file.extension
file.contentType
file.lastModifiedTime     // returns JSR-310 time
file.owner
file.group
file.isDirectory
file.isHidden
file.hide() / file.unhide()
file.isOwnerExecutable/file.isGroupReadable // see file.permissions
file.size                 // for a directory, computes the directory size
```

**Equality**: Use `==` to check for path-based equality and `===` for content-based equality
```scala
file1 == file2    // true iff both point to same path on the filesystem
file1 === file2   // true iff both have same contents (works for BOTH regular-files and directories)
```
<!---
**Zip APIs**: You don't have to lookup on StackOverflow "How to zip/unzip in Java/Scala?":
```scala
val zipFile = file"path/to/research.zip"
val documents = home / "Documents"
val research: File = zipFile.unzipTo(documents / "research")    // Unzip
```
You can also cleverly use the extractors above: `val Directory(docs) = zipFile.unzipTo(documents / "research")`
--->

For **more examples**, consult the [tests](src/test/scala/better/FilesSpec.scala).

**sbt**: In your `build.sbt`, add this:
```scala
resolvers += Resolver.bintrayRepo("pathikrit", "maven")

libraryDependencies += "com.github.pathikrit" %% "better-files" % "0.0.1"
```

**Future work**:
* File watchers using Akka actors
* Classpath resource APIs
* Zip APIs
* CSV handling
* File converters
* gitter.im
