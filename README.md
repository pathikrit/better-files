better-files [![Circle CI](https://img.shields.io/circleci/project/pathikrit/better-files.svg)](https://circleci.com/gh/pathikrit/better-files) [![Download](https://api.bintray.com/packages/pathikrit/maven/better-files/images/download.svg)](https://bintray.com/pathikrit/maven/better-files/_latestVersion)
--------
better-files is a [dependency-free](build.sbt) idiomatic [thin Scala wrapper](src/main/scala/better/files/package.scala) around Java file APIs:

**Concepts**: Just 2 concepts - `Path` and `File`:
* `better.files.Path` is Scala wrapper for `java.nio.file.Path` and 
* `better.files.File` is Scala wrapper for `java.io.File`

These instances can be interchangeably used with Java classes via automatic implicit conversions from Java to Scala and vice-versa

**Instantiation**: The following are all equivalent:
```scala
import better.files._

val f1: File = file"/User/johndoe/Documents"
val f2: File = root / "User" / "johndoe" / "Documents"
val f3: File = home / "Documents"
val f4: File = new java.io.File("/User/johndoe/Documents")
```

**I/O**: Dead simple I/O:
```scala
val file = root / "tmp" / "test.txt"
file < "hello"    // file.overwrite("hello") if you don't like symbols
file << "world"   // file.append("world") if you don't like symbols
assert(file.contents() == "hello\nworld")
```
All operations are chainable e.g.
```scala
assert((file < "hello" << "world").contents() == "hello\nworld\n")
```

For **more examples**, consult the [tests](src/test/scala/better/FilesSpec.scala).

**sbt**: In your `build.sbt`, add this:
```scala
resolvers += Resolver.bintrayRepo("pathikrit", "maven")

libraryDependencies += "com.github.pathikrit" %% "better-files" % "0.0.1"
```

**TODO**
* extractors: http://stackoverflow.com/questions/32518393
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
* doc
