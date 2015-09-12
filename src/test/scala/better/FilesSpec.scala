package better

import better.files.File

import java.nio.file._

import org.scalatest.{BeforeAndAfter, FlatSpec}

class FilesSpec extends FlatSpec with BeforeAndAfter {
  val root: File = Files.createTempDirectory("better-files").toFile

  /**
   * Setup the following directory structure under root
   * /a
   * /a1
   * /a2
   * a21.txt
   * a22.txt
   * /b
   */
  before {
    Seq(
      root / "a" / "a1",
      root / "a" / "a2",
      root / "b"
    ).foreach(_.mkdirs())
  }

  after {
    //root.de
  }

  "files" can "be instantiated" in {

  }
}
