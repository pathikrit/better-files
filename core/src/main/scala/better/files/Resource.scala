package better.files

import scala.reflect.macros.blackbox

private[files] final class Resource(val c: blackbox.Context) {
  import c.universe._

  private[this] def threadContextClassLoader =
    q"_root_.java.lang.Thread.currentThread.getContextClassLoader"

  def file(name: Tree): Tree =
    q"_root_.better.files.File($threadContextClassLoader.getResource($name))"

  def stream(name: Tree): Tree =
    streamBuffered(name, q"_root_.better.files.DefaultBufferSize")

  def streamBuffered(name: Tree, bufferSize: Tree): Tree =
    q"new _root_.java.io.BufferedInputStream($threadContextClassLoader.getResourceAsStream($name), $bufferSize)"
}
