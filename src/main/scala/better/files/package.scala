package better

import java.io.{File => JFile}
import java.nio.charset.Charset
import java.nio.file.{Path => JPath, StandardOpenOption, Files, Paths}, StandardOpenOption._

import scala.collection.JavaConversions._

package object files {
  /**
   * Scala wrapper for java.io.File
   */
  implicit class File(val file: JFile) {
    def path: Path = file.toPath

    def /(child: String): File = new JFile(file, child)

    def append(lines: String*): File = Files.write(this, lines, APPEND, CREATE)

    def <<(line: String): File = append(line)

    def write(text: String): File = Files.write(this, text)

    val overwrite = write _
    //TODO: use method alias
    val < = write _

    def contents: Array[Byte] = Files.readAllBytes(this)

    def contents(charset: Charset = Charset.defaultCharset()): String = new String(contents, charset)
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

  implicit def fileToJavaFile(file: File): JFile = file.file //TODO: Use bidirectional implicits?

  implicit def fileToJavaPath(file: File): JPath = fileToPath(file)

  implicit def javaPathToFile(path: JPath): File = pathToFile(path)

  private[this] implicit def stringToBytes(s: String): Array[Byte] = s.getBytes
}
