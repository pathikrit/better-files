package better.files

import better.files.Dsl._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class GlobSpec extends FlatSpec with BeforeAndAfterAll with Matchers {
  val isUnixOS = sys.props.get("os.name") match {
    case Some("Linux" | "MaxOS") => true
    case _ => false
  }

  var testDir: File = _
  var globTree: File = _
  var specialTree: File = _

  var regexWildcardPath: File = _
  var globWildcardPath: File = _
  //
  //  Test target for glob
  //
  //      tests/
  //      ├── globtree
  //      │   ├── a
  //      │   │   ├── a2
  //      │   │   │   ├── a2.txt
  //      │   │   │   └── x.txt
  //      │   │   ├── a.not
  //      │   │   ├── a.txt
  //      │   │   └── x.txt
  //      │   ├── b
  //      │   │   ├── a
  //      │   │   │   └── ba.txt
  //      │   │   └── b.txt
  //      │   ├── c
  //      │   │   ├── c.txt
  //      │   │   └── x.txt
  //      │   ├── empty
  //      │   ├── link_to_a -> a
  //      │   ├── one.txt
  //      │   ├── readme.md
  //      │   ├── three.txt
  //      │   └── two.txt
  //      └── special
  //          ├── .*
  //          │   └── a
  //          │       └── a.txt
  //          └── **
  //              └── a
  //                  └── a.txt
  //
  override def beforeAll(): Unit = {
    testDir = File.newTemporaryDirectory("glob-tests")
    globTree = testDir / "globtree"

    mkdir(globTree)
    val a = mkdir(globTree / "a" )
    mkdir(globTree / "a" / "a2")
    touch(globTree / "a" / "a2" / "a2.txt")
    touch(globTree / "a" / "a2" / "x.txt")
    touch(globTree / "a" / "a.not")
    touch(globTree / "a" / "a.txt")
    touch(globTree / "a" / "x.txt")

    mkdir(globTree / "b" )
    mkdir(globTree / "b" / "a")
    touch(globTree / "b" / "a" / "ba.txt")
    touch(globTree / "b" / "b.txt")

    mkdir(globTree / "c" )
    touch(globTree / "c" / "c.txt")
    touch(globTree / "c" / "x.txt")

    mkdir(globTree / "empty" )

    if (isUnixOS) {
      ln_s(globTree / "link_to_a", a)
    }

    touch(globTree / "one.txt")
    touch(globTree / "two.txt")
    touch(globTree / "three.txt")
    touch(globTree / "readme.md")

    // Special target with path name components as wildcards
    specialTree = testDir / "special"

    // regex
    mkdir(specialTree)
    regexWildcardPath = mkdir(specialTree / ".*" )
    mkdir(specialTree / ".*" / "a" )
    touch(specialTree / ".*" / "a" / "a.txt")

    // glob
    globWildcardPath = mkdir(specialTree / "**" )
    mkdir(specialTree / "**" / "a" )
    touch(specialTree / "**" / "a" / "a.txt")

    ()
  }

  override def afterAll(): Unit = {
    rm(testDir)
    ()
  }

  /**
   * Helper in case something goes wrong...
   */
  private def printpaths(files: Seq[File]) = {
    println("SIZE: " + "%d".format(files.size))
    files.sortBy(_.path)
      .foreach(p => {
        println("PATH: " + p.toString)
      })
  }

  /**
   * Verity if candidates are equal with references.
   * Does not accept empty sets, use assert(paths.isEmpty) for that.
   *
   * @param pathsIt candidates
   * @param refPaths references
   * @param baseDir basedir to for creating full path of references
   * @return true if candidates are identical with references
   */
  private def verify(pathsIt: Files, refPaths: Seq[String], baseDir: File) = {
    val paths = pathsIt.toSeq
    val refs = refPaths
      .map(refPath => {baseDir / refPath})
      .sortBy(_.path)

    val result = (paths.length == refPaths.length) && paths.nonEmpty &&
      paths.sortBy(_.path)
        .zip(refs)
        .forall({ case (path, refPath) => path === refPath})

    if (result == false) {
      println("result:")
      printpaths(paths)
      println("refs:")
      printpaths(refs)
    }
    result
  }

  "glob" should "match plain file (e.g. 'file.ext')" in {
    val refPaths = Seq(
      "one.txt")

    val paths = globTree.glob("one.txt")

    verify(paths, refPaths, globTree) should be(true)
  }
  it should "match path without glob (e.g. 'sub/dir/file.ext')" in {
    val refPaths = Seq(
      "a/a.txt")

    val paths = globTree.glob("a/a.txt")

    verify(paths, refPaths, globTree) should be(true)
  }

  it should "match file-glob (e.g. '*.ext')" in {
    val refPaths = Seq(
      "one.txt",
      "two.txt",
      "three.txt")

    val paths = globTree.glob("*.txt")

    verify(paths, refPaths, globTree) should be(true)

    assert(globTree.glob("*.txt")(File.PathMatcherSyntax.glob).isEmpty)
  }

  it should "match fixed sub dir and file-glob  (e.g. '**/subdir/*.ext')" in {
    // TODO: DOC: why top level 'a' is not matched
    val refPaths = List(
      "b/a/ba.txt")

    val paths = globTree.glob("**/a/*.txt")

    verify(paths, refPaths, globTree) should be(true)
  }

  it should "use parent dir for matching (e.g. plain 'subdir/*.ext')" in {
    // e.g. check that b nor c are matched, nor b/a
    val refPaths = Seq(
      "a/a.txt",
      "a/x.txt")

    val paths = globTree.glob("a/*.txt")

    verify(paths, refPaths, globTree) should be(true)
  }

  it should "match sub-directory glob with plain file (e.g. 'subdir/*/file.ext')" in {
    val refPaths = Seq(
      "a/x.txt",
      "c/x.txt")

    val paths = testDir.glob("globtree/*/x.txt")

    verify(paths, refPaths, globTree) should be(true)
  }

  it should "match sub-directory glob with file-glob (e.g. 'subdir/*/*.ext')" in {
    val refPaths = Seq(
      "a/a.txt",
      "a/x.txt",
      "c/c.txt",
      "c/x.txt",
      "b/b.txt")

    val paths = testDir.glob("globtree/*/*.txt")

    verify(paths, refPaths, globTree) should be(true)
  }

  it should "match deep sub-directory glob with plain file (e.g. 'subdir/**/file.ext')" in {
    val refPaths = Seq(
      "a/a2/x.txt",
      "a/x.txt",
      "c/x.txt")

    val p1s = globTree.glob("**/x.txt")
    verify(p1s, refPaths, globTree) should be(true)

    val p2s = testDir.glob("globtree/**/x.txt")
    verify(p2s, refPaths, globTree) should be(true)
  }

  it should "match deep sub-directory glob with file-glob (e.g. 'subdir/**/*.ext')" in {
    val refPaths = Seq(
      "a/a.txt",
      "a/x.txt",
      "a/a2/x.txt",
      "a/a2/a2.txt",
      "c/x.txt",
      "c/c.txt",
      "b/b.txt",
      "b/a/ba.txt")

    val p1s = globTree.glob("**/*.txt")
    verify(p1s, refPaths, globTree) should be(true)

    val p2s = testDir.glob("globtree/**/*.txt")
    verify(p2s, refPaths, globTree) should be(true)
  }

  it should "match deep file-glob (e.g. 'subdir/**.ext')" in {
    val refPaths = Seq(
      "one.txt",
      "two.txt",
      "three.txt",
      "a/a.txt",
      "a/x.txt",
      "a/a2/x.txt",
      "a/a2/a2.txt",
      "b/a/ba.txt",
      "b/b.txt",
      "c/x.txt",
      "c/c.txt")

    val p1s = globTree.glob("**.txt")
    verify(p1s, refPaths, globTree) should be(true)

    val p2s = testDir.glob("globtree/**.txt")
    verify(p2s, refPaths, globTree) should be(true)
  }

  it should "match everything (e.g. 'subdir/**')" in {
    val refPaths = List(
      "a",
      "a/a.not",
      "a/a.txt",
      "a/a2",
      "a/a2/a2.txt",
      "a/a2/x.txt",
      "a/x.txt",
      "b",
      "b/a",
      "b/a/ba.txt",
      "b/b.txt",
      "c",
      "c/c.txt",
      "c/x.txt",
      "empty",
      "one.txt",
      "readme.md",
      "three.txt",
      "two.txt") ++
      (if (isUnixOS) {
        List("link_to_a")
      }
      else {
        Nil
      })

    val paths = testDir.glob("globtree/**")

    verify(paths, refPaths, globTree) should be(true)
  }

  it should "work with links (e.g. 'link_to_a/**.txt')" in {
    assume(isUnixOS)
    val refPaths = Seq(
      "a/a.txt",
      "a/x.txt",
      "a/a2/x.txt",
      "a/a2/a2.txt")

    // TODO: DOC: File behaviour, links are resolved (abs + normalized path)

    val p1s = globTree.glob("link_to_a/**.txt")(visitOptions = File.VisitOptions.follow)
    verify(p1s, refPaths, globTree) should be(true)

    val p2s = globTree.glob("link_to_a/**.txt").toSeq
    assert(p2s.isEmpty)

    val p3s = testDir.glob("globtree/link_to_a/**.txt")(visitOptions = File.VisitOptions.follow)
    verify(p3s, refPaths, globTree) should be(true)

    val p4s = testDir.glob("globtree/link_to_a/**.txt")
    assert(p4s.isEmpty)
  }

  it should "not use dir name as wildcard (e.g. dirname is **)" in {

    val d = globWildcardPath // "path" / "with" / "**"
    val paths = d.glob("*.txt")

    assert(paths.isEmpty)
  }



  "Regex" should "match all txt-files  under sub-directory (e.g. '.*/.*\\\\.txt')" in {
    val refPaths = Seq(
      "a/a.txt",
      "a/x.txt",
      "a/a2/x.txt",
      "a/a2/a2.txt",
      "c/x.txt",
      "c/c.txt",
      "b/b.txt",
      "b/a/ba.txt")

    val paths = globTree.glob(".*/.*\\.txt")(File.PathMatcherSyntax.pathRegex)

    verify(paths, refPaths, globTree) should be(true)
  }

  it should "use parent dir for matching (e.g. plain 'subdir/*.ext' instead of '**/subdir/*.ext)" in {
    // e.g. check that b nor c are matched, nor b/a
    val refPaths = Seq(
      "a/a.txt",
      "a/x.txt",
      "a/a2/a2.txt",
      "a/a2/x.txt")

    val paths = globTree.glob("a/.*\\.txt")(File.PathMatcherSyntax.pathRegex)

    verify(paths, refPaths, globTree) should be(true)

    assert(globTree.glob("a/.*\\.txt")(File.PathMatcherSyntax.regex).isEmpty)
  }

  it should "not use dir name as wildcard (e.g. dirname is .*)" in {

    val d = regexWildcardPath // "path" / "with" / ".*"
    val paths = d.glob("a\\.txt")(File.PathMatcherSyntax.pathRegex)

    assert(paths.isEmpty)
  }
}
