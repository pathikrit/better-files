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
    val cl = new URLClassLoader(Array(clURL))

    assert(Resource.from(cl).getAsString(testFileFromCL) startsWith testFileText)
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
    def testClass: Class[_] = Class forName "better.files.File"

    assert(Resource.at(testClass).getAsString(testFileRel) startsWith testFileText)
  }

  it can "look up a file in another package" in {
    assert(Resource.at[ResourceSpecHelper].getAsString(testFileAltRel) startsWith altTestFileText)
  }

  "Resource.at" should "require a concrete type" in {
    """def foo[T] = better.files.Resource.at[T].asStream("foo")""" shouldNot typeCheck
  }
}
