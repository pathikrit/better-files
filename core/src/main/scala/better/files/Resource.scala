package better.files

import java.io.InputStream

import scala.reflect.macros.blackbox

/**
  * Class to encapsulate resource related APIs
  * See: https://stackoverflow.com/questions/3861989/preferred-way-of-loading-resources-in-java
  */
object Resource {
  def apply(name: String): InputStream =
    macro Macros.applyImpl

  /**
    * If bufferSize is set to less than or equal to 0, we don't buffer
    * @param bufferSize
    * @return
    */
  def apply(name: String, bufferSize: Int): InputStream =
    macro Macros.applyBufferedImpl

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
      applyBufferedImpl(name, q"_root_.better.files.DefaultBufferSize") //Macros do not support default params

    def applyBufferedImpl(name: Tree, bufferSize: Tree): Tree =
      q"$threadContextClassLoader.getResourceAsStream($name).buffered($bufferSize)"
  }
}
