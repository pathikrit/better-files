package better.files

import scala.reflect._

object ContextClassLoader {
  def apply(): ClassLoader =
    macro ContextClassLoader.applyImpl

  def of[T](implicit ct: ClassTag[T]): ClassLoader =
    ct.runtimeClass.getClassLoader
}

class ContextClassLoader(val c: macros.blackbox.Context) {
  import c.universe._
  def applyImpl(): Tree =
    q"_root_.java.lang.Thread.currentThread.getContextClassLoader"
}
