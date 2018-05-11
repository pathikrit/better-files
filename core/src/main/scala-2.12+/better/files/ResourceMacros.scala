package better.files

import scala.reflect.macros.{ReificationException, blackbox}

/**
  * Implementations of the `Resource.at` macros. This is needed because `Class#getResource` is caller-sensitive; calls to it must appear in user code, ''not'' in better-files.
  *
  * This is the Scala 2.12+ version. It implements ResourceLoader with a lambda, instead of an inner class.
  */
private[files] final class ResourceMacros(val c: blackbox.Context) {
  import c.universe._
  import c.Expr

  def atStaticImpl[T](implicit T: WeakTypeTag[T]): Expr[ResourceLoader] = {
    val rtc = Expr[Class[_]] {
      try c.reifyRuntimeClass(T.tpe, concrete = true)
      catch {
        case _: ReificationException =>
          c.abort(c.enclosingPosition, s"${T.tpe} is not a concrete type")
      }
    }

    atDynamicImpl(rtc)
  }

  def atDynamicImpl(clazz: Expr[Class[_]]): Expr[ResourceLoader] =
    reify {
      val clazz_ = clazz.splice
      ((name: String) => Option(clazz_.getResource(name))): ResourceLoader
    }

  def myImpl: Expr[ResourceLoader] = {
    val rtc = c.reifyEnclosingRuntimeClass

    if (rtc.isEmpty)
      // The documentation for reifyEnclosingRuntimeClass claims that this is possible, somehow. I have no idea where a macro call could possibly appear that's not inside a scope that compiles to a class file, but I guess we'll have to deal with it.
      c.abort(c.enclosingPosition,
              "cannot use ‘my’ here, because this location doesn't correspond to a Java class file")

    atDynamicImpl(Expr[Class[_]](rtc))
  }
}
