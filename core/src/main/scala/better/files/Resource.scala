package better.files

import java.io.InputStream
import java.net.URL

import scala.reflect.macros.blackbox

/**
  * Class to encapsulate resource related APIs
  * See: https://stackoverflow.com/questions/3861989/preferred-way-of-loading-resources-in-java
  */
object Resource {
  def apply(name: String): InputStream =
    macro Macros.applyImpl

  def url(name: String): URL =
    macro Macros.urlImpl

  /**
    * Get a file from a resource
    * Note: Use resourceToFile instead as this may not actually always load the file
    *
    * @param name
    * @return
    */
  def asFile(name: String): File =
    macro Macros.asFileImpl

  /**
    * Why do we need macros?
    * See: https://github.com/pathikrit/better-files/pull/227
    */
  private[this] class Macros(val c: blackbox.Context) {
    import c.universe._

    private[this] def threadContextClassLoader =
      q"_root_.java.lang.Thread.currentThread.getContextClassLoader"

    def asFileImpl(name: Tree): Tree =
      q"_root_.better.files.File($threadContextClassLoader.getResource($name))"

    def applyImpl(name: Tree): Tree =
      q"$threadContextClassLoader.getResourceAsStream($name)"

    def urlImpl(name: Tree): Tree =
      q"$threadContextClassLoader.getResource($name)"
  }
}
