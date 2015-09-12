better-files [![Circle CI](https://circleci.com/gh/pathikrit/better-files.svg?style=svg)](https://circleci.com/gh/pathikrit/better-files) [![Download](https://api.bintray.com/packages/pathikrit/maven/better-files/images/download.svg)](https://bintray.com/pathikrit/maven/better-files/_latestVersion)
--------
better-files is a [dependency-free](build.sbt) idiomatic [thin Scala wrapper](src/main/scala/better/files/package.scala) around Java file APIs 
that can be interchangeably used with Java classes via automatic implicit conversions from Java to Scala and vice-versa.

**Instantiation**: The following are all equivalent:
```scala
import better.files._

val f = File("/User/johndoe/Documents")
val f1: File = file"/User/johndoe/Documents"
val f2: File = root / "User" / "johndoe" / "Documents"
val f3: File = home / "Documents"
val f4: File = new java.io.File("/User/johndoe/Documents")
val f5: File = "/User/johndoe/Documents".toFile
```

**I/O**: Dead simple I/O:
```scala
val file = root / "tmp" / "test.txt"
file.overwrite("hello")
file.append("world")
assert(file.contents() == "hello\nworld")
```
If you are someone who likes symbols, then the above code can also be written as:
```scala
file < "hello"
file << "world"
```
Or even, right-associative ones:
```scala
"hello" >: file
"world" >>: file 
```
All operations are chainable e.g.
```scala
assert((file < "hello" << "world").contents() == "hello\nworld\n")
```

**Powerful pattern matching**: Instead of `if-else`, more readable Scala pattern matching:
```scala
file"src/test/foo" match {
  case SymbolicLink(to) =>          //this must be first case statement if you want to handle symlinks specially; else will follow link
  case Directory(children) => 
  case RegularFile(contents) => 
  case other if other.exists() =>   //A file may not be one of the above e.g. UNIX pipes, sockets, devices etc
  case _ =>                         //A file that does not exist
}
```

For **more examples**, consult the [tests](src/test/scala/better/FilesSpec.scala).

**sbt**: In your `build.sbt`, add this:
```scala
resolvers += Resolver.bintrayRepo("pathikrit", "maven")

libraryDependencies += "com.github.pathikrit" %% "better-files" % "0.0.1"
```

**TODO**
* remove path?
* touch
* createIfNotExists
* readAttributes incl. lastModifiedTime, owner, permissions, contentType, hidden?
* File.temp()
* name(excludeExtension = true)
* parent or '..'
* extension
* file.lines
* copyTo 
* moveTo
* watch
* contentEquals
* size
* delete - true, false, nosuchfile?
* list()
* glob()
* checkSum
* all above works for dirs too
* code coverage
* version-eye
* code doc
* tut?
* method alias, bi-directional implicits
