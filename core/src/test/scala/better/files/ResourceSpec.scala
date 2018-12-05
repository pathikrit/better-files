package better.files

import java.net.{URL, URLClassLoader}

import better.files.test_pkg.ResourceSpecHelper

final class ResourceSpec extends CommonSpec {
  implicit val charset = java.nio.charset.StandardCharsets.US_ASCII
  val testFileText     = "This is the test-file.txt file."
  val altTestFileText  = "This is the another-test-file.txt file."
  val testFile         = "better/files/test-file.txt"
  val testFileRel      = "test-file.txt"
  val testFileAltRel   = "another-test-file.txt"
  val testFileFromCL   = "files/test-file.txt"

  "Resource" can "look up from the context class loader" in {
    assert(Resource.asStream(testFile).get.asString() startsWith testFileText)
  }

  it can "look up from a specified class loader" in {
    val clURL = new URL(Resource.my.getUrl("ResourceSpec.class"), "../")
    assert(clURL.toExternalForm endsWith "/")
    assert(Resource.from(new URLClassLoader(Array(clURL))).getAsString(testFileFromCL) startsWith testFileText)
  }

  it can "look up from the call site" in {
    assert(Resource.my.asStream(testFileRel).get.asString() startsWith testFileText)
    // This tests that Resource.my uses the correct call site when called from outside the better.files package.
    assert((new ResourceSpecHelper).openTestStream().asString() startsWith altTestFileText)
  }

  it can "look up from a statically-known type" in {
    assert(Resource.at[ResourceSpec].getAsString(testFileRel) startsWith testFileText)
    assert(Resource.at[Resource.type].getAsString(testFileRel) startsWith testFileText)
  }

  it can "look up from a java.lang.Class" in {
    assert(Resource.at(Class.forName("better.files.File")).getAsString(testFileRel) startsWith testFileText)
  }

  it can "look up a file in another package" in {
    assert(Resource.at[ResourceSpecHelper].getAsString(testFileAltRel) startsWith altTestFileText)
  }

  it should "require a concrete type" in {
    """def foo[T] = better.files.Resource.at[T].asStream("foo")""" shouldNot typeCheck
  }

  it should "fetch root url" in {
    assert(Option(Resource.getUrl()).isDefined)
  }

  it should "work with using util" in {
    File.usingTemporaryFile() { file =>
      file.appendText("hello world")
      val lines = using(file.newInputStream) { is =>
        is.lines.toList
      }
      assert(lines === "hello world" :: Nil)
    }
  }

  it should "close multiple resources" in {
    def emit(dir: File, partitions: Int, lines: Int) = {
      for {
        writers <- Vector.tabulate(partitions)(i => (dir / s"partition-$i.csv").newPrintWriter()).autoClosed
        line    <- 1 to lines
      } writers(line % partitions).println(line)
    }

    File.usingTemporaryDirectory() { dir =>
      val lines = 1000
      emit(dir = dir, partitions = 5, lines = lines)
      val expected = dir.list(filter = _.extension.contains(".csv")).flatMap(_.lines).map(_.toInt).toSet
      assert((1 to lines).forall(expected))
    }
  }
}
