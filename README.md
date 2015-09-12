better-files [![Circle CI](https://img.shields.io/circleci/project/pathikrit/better-files.svg)](https://circleci.com/gh/pathikrit/better-files) [![Download](https://api.bintray.com/packages/pathikrit/maven/better-files/images/download.svg)](https://bintray.com/pathikrit/maven/better-files/_latestVersion)
--------

better-files is a thin Scala wrapper around Java file APIs:

**Usage**: 
Just add this import to use better-files in your Scala code:
```scala
import better.files._
```
Two main concepts:
`better.files.Path` is Scala wrapper for `java.nio.file.Path` and 
`better.files.File` is Scala wrapper for `java.io.File`

There are many ways to refer to a file. The following are all equivalent:
```scala
val f1 = file"/User/johndoe/Documents"
val f2 = / 'User / 'johndoe / 'Documents
val f3 = ~/ 'johndoe / 'Documents
```

better-file instances are 100% compatible with Java file classes through automatic implicit conversions from Java to Scala and vice-versa:
```scala
def append(file: java.util.File, text: String): Unit = {
  file >> text    // If you are not a fan of symbols, you can also do file.append(text)
}
```

**sbt**: In your `build.sbt`, add this:
```scala
resolvers += Resolver.bintrayRepo("pathikrit", "maven")

libraryDependencies += "com.github.pathikrit" %% "better-files" % "0.0.1"
```




