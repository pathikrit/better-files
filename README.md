better-files [![Circle CI](https://img.shields.io/circleci/project/pathikrit/better-files.svg)](https://circleci.com/gh/pathikrit/better-files) [![Download](https://api.bintray.com/packages/pathikrit/maven/better-files/images/download.svg)](https://bintray.com/pathikrit/maven/better-files/_latestVersion)
--------

better-files is a [dependency-free](build.sbt) [thin Scala wrapper](src/main/scala/better/files/package.scala) around Java file APIs:

**Usage**: 
Just add this import to use better-files in your Scala code:
```scala
import better.files._
```
Two main concepts:

`better.files.Path` is Scala wrapper for `java.nio.file.Path` and 

`better.files.File` is Scala wrapper for `java.io.File`

better-file instances are 100% compatible and interchangeably used with Java file classes through automatic implicit conversions from Java to Scala and vice-versa.

There are many ways to refer to a file. The following are all equivalent:
```scala
val f1 = file"/User/johndoe/Documents"
val f2 = / "User" / "johndoe" / "Documents"
val f3 = ~/ "johndoe" / "Documents"
```

Dead simple I/O:
```scala
val file = / "tmp" / "test.txt"
file < "hello"    // file.overwrite("hello")
file << "world"   // file.append("world")
file.contents() == "hello\nworld"
```

For more examples, consult the [tests](src/test/scala/better/FileSpec.scala)

**sbt**: In your `build.sbt`, add this:
```scala
resolvers += Resolver.bintrayRepo("pathikrit", "maven")

libraryDependencies += "com.github.pathikrit" %% "better-files" % "0.0.1"
```
