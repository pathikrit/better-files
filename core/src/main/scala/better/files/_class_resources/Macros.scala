package better.files
package _class_resources

import scala.reflect.macros.blackbox

private[files] final class Macros(val c: blackbox.Context) {
  import c.universe._

  private[this] def ccl =
    q"_root_.java.lang.Thread.currentThread.getContextClassLoader"

  def file(name: Tree): Tree =
    q"_root_.better.files.File($ccl.getResource($name))"

  def stream(name: Tree): Tree =
    streamBuf(name, q"_root_.better.files.DefaultBufferSize")

  def streamBuf(name: Tree, bufferSize: Tree): Tree =
    q"new _root_.java.io.BufferedInputStream($ccl.getResourceAsStream($name), $bufferSize)"
}
