package better

import java.io.{File => JFile}
import java.nio.file.{Path => JPath, Files, Paths}

package object files {

  /**
   * Scala wrapper for java.io.File
   */
  implicit class File(val file: JFile) {
    def path: Path = file.toPath

    def /(child: String): File = new JFile(file, child)
  }

  /**
   * Scala wrapper for java.nio.file.Path
   */
  implicit class Path(val path: JPath) {
    def file: File = path.toFile

    def /(name: String): Path = path.resolve(name)
  }

  /**
   * Root path
   */
  val / : Path = File(JFile.listRoots().head)

  implicit class StringInterpolations(sc: StringContext) {
    def file(args: Any*): File = pathToFile(Paths.get(sc.s(args: _*)))
  }

  implicit def pathToFile(path: Path): File = path.file

  implicit def fileToPath(file: File): Path = file.path

  implicit def pathToJavaPath(path: Path): JPath = path.path

  implicit def fileToJavaFile(file: File): JFile = file.file
}
