[![Circle CI](https://circleci.com/gh/pathikrit/better-files.svg?style=svg&circle-token=3800512b1d901f1cf24538b392df471117d40cfb)](https://circleci.com/gh/pathikrit/better-files)
[![Dependency Status](https://www.versioneye.com/user/projects/55f5e7de3ed894001e0003b1/badge.svg?style=flat)](https://www.versioneye.com/user/projects/55f5e7de3ed894001e0003b1)
[![Codacy](https://api.codacy.com/project/badge/0e2aeb7949bc49e6802afcc43a7a1aa1)](https://www.codacy.com/app/pathikrit/better-files/dashboard) 
[![Download](https://api.bintray.com/packages/pathikrit/maven/better-files/images/download.svg)](https://bintray.com/pathikrit/maven/better-files/_latestVersion)

**better-files** is a [dependency-free](build.sbt) idiomatic [thin Scala wrapper](src/main/scala/better/files/package.scala) around Java file APIs 
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

**I/O**: Dead simple I/O (via [Java NIO](https://en.wikipedia.org/wiki/Non-blocking_I/O_(Java)):
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
All operations are chainable e.g.`(file < "hello" << "world").read()`

**Powerful pattern matching**: Instead of `if-else`, more readable Scala pattern matching:
```scala
"src" / "test" / "foo" match {
  case SymbolicLink(to) =>          //this must be first case statement if you want to handle symlinks specially; else will follow link
  case Directory(children) => 
  case RegularFile(contents) => 
  case other if other.exists() =>   //A file may not be one of the above e.g. UNIX pipes, sockets, devices etc
  case _ =>                         //A file that does not exist
```

**Globbing**: You don't have to Google "How to glob in Java/Scala" 
and try to port [this](http://docs.oracle.com/javase/tutorial/essential/io/find.html) to Scala next time:
```scala
val matches: Set[File] = ("src" / "test").glob("**/*.{java,scala}")
```
You can even use the more advanced regex syntax instead of glob syntax:
```scala
val matches = ("src" / "test").glob("**/*.{java,scala}", syntax = "regex")
```
For simpler cases, you can always use `dir.list` or `dir.listRecursively`

**File attribute APIs**: Query various file attributes e.g.:
```scala
file.attrs.lastModifiedTime     // returns JSR-310 time
file.attrs.owner
file.attrs.permissions
file.attrs.contentType
file.attrs.isHidden
file.size                       // for a directory, computes the directory size
```

**File-system operations**: Utilities to `cp`, `rm`, `ls`, `mv`, `md5`, `diff`, `touch` etc:
```scala
file.name       // simpler than java.io.File#getname
file.touch
file.extension
file.readLines 
file.delete     // unlike the Java API, also works on directories as expected
file.moveTo(destination)
file.copyTo(destination)
file.checksum
File.newTempDir()
File.newTempFile()
```

**Equality**: Use `==` to check for path-based equality and `===` for content-based equality
```scala
file1 == file2    // true iff both point to same path on the filesystem
file1 === file2   // true iff both have same contents (works for BOTH regular-files and directories)
```

For **more examples**, consult the [tests](src/test/scala/better/FilesSpec.scala).

**sbt**: In your `build.sbt`, add this:
```scala
resolvers += Resolver.bintrayRepo("pathikrit", "maven")

libraryDependencies += "com.github.pathikrit" %% "better-files" % "0.0.1"
```

**TODO**
* watch
* resource stringcontext
