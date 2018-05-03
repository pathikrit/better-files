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
    assert(Resource.asFile(testFile).contentAsString startsWith testFileText)
    assert(Resource(testFile).asString() startsWith testFileText)
    assert(File(Resource.url(testFile)).contentAsString startsWith testFileText)
  }

  it can "look up from a specified class loader" in {
    val clURL = new URL(Resource.my.url("ResourceSpec.class"), "../")
    assert(clURL.toExternalForm endsWith "/")
    val cl = new URLClassLoader(Array(clURL))

    assert(Resource.from(cl).asFile(testFileFromCL).contentAsString startsWith testFileText)
    assert(Resource.from(cl)(testFileFromCL).asString() startsWith testFileText)
    assert(File(Resource.from(cl).url(testFileFromCL)).contentAsString startsWith testFileText)
  }

  it can "look up from the call site" in {
    assert(Resource.my.asFile(testFileRel).contentAsString startsWith testFileText)
    assert(Resource.my(testFileRel).asString() startsWith testFileText)
    assert(File(Resource.my.url(testFileRel)).contentAsString startsWith testFileText)
  }

  it can "look up from a statically-known type" in {
    assert(Resource.at[FileSpec].asFile(testFileRel).contentAsString startsWith testFileText)
    assert(Resource.at[FileSpec](testFileRel).asString() startsWith testFileText)
    assert(File(Resource.at[FileSpec].url(testFileRel)).contentAsString startsWith testFileText)
  }

  it can "look up from a java.lang.Class" in {
    def testClass: Class[_] = Class forName "better.files.File"

    assert(Resource.at(testClass).asFile(testFileRel).contentAsString startsWith testFileText)
    assert(Resource.at(testClass)(testFileRel).asString() startsWith testFileText)
    assert(File(Resource.at(testClass).url(testFileRel)).contentAsString startsWith testFileText)
  }

  it can "look up a file in another package" in {
    assert(Resource.at[ResourceSpecHelper].asFile(testFileAltRel).contentAsString startsWith altTestFileText)
    assert(Resource.at[ResourceSpecHelper](testFileAltRel).asString() startsWith altTestFileText)
    assert(File(Resource.at[ResourceSpecHelper].url(testFileAltRel)).contentAsString startsWith altTestFileText)
  }

  "Resource.at" should "require a concrete type" in {
    """def foo[T] = better.files.Resource.at[T]("foo")""" shouldNot typeCheck
  }

  "Resource.my" should "look up from the call site" in {
    assert((new ResourceSpecHelper).myTestFile.contentAsString startsWith altTestFileText)
  }
}
