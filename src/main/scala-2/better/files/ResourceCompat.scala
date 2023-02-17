package better.files

import scala.reflect.macros.{blackbox, ReificationException}

private[files] trait ResourceCompat {

  /** Look up class resource files.
    *
    * This Resource looks up resources relative to the JVM class file for `T`,
    * using [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]].
    * For example, if `com.example.ExampleClass` is given for `T`, then resource files will be searched for in the `com/example` folder containing `ExampleClass.class`.
    *
    * If you want to look up resource files relative to the call site instead (that is, you want a class to look up one of its own resources), use the `my` method instead.
    *
    * @example {{{ Resource.at[YourClass].url("config.properties") }}}
    * @tparam T The class, trait, or object to look up from. Objects must be written with a `.type` suffix, such as `Resource.at[SomeObject.type]`.
    * @return A Resource for `T`.
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
    */
  def at[T]: Resource = macro Macros.atStaticImpl[T]

  /** Look up class resource files.
    *
    * This Resource looks up resources from the given Class,
    * using [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]].
    * For example, if `classOf[com.example.ExampleClass]` is given for `clazz`, then resource files will be searched for
    * in the `com/example` folder containing `ExampleClass.class`.
    *
    * If you want to look up resource files relative to the call site instead (that is, you want your class to look up one of its own resources),
    * use the `my` method instead.
    *
    * @example {{{ Resource.at(Class.forName("your.AppClass")).url("config.properties") }}}
    *
    * In this example, a file named `config.properties` is expected to appear alongside the file `AppClass.class` in the package `your`.
    * @param clazz The class to look up from.
    * @return A Resource for `clazz`.
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
    */
  def at(clazz: Class[_]): Resource = macro Macros.atDynamicImpl

  /** Look up own resource files.
    *
    * This Resource looks up resources from the [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html Class]] surrounding the call,
    * using [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]].
    * For example, if `my` is called from `com.example.ExampleClass`,
    * then resource files will be searched for in the `com/example` folder containing `ExampleClass.class`.
    *
    * @example {{{ Resource.my.url("config.properties") }}}
    * @return A Resource for the call site.
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
    */
  def my: Resource = macro Macros.myImpl
}

/** Implementations of the `Resource.at` macros. This is needed because `Class#getResource` is caller-sensitive;
  * calls to it must appear in user code, ''not'' in better-files.
  */
private[files] final class Macros(val c: blackbox.Context) {

  import c.Expr
  import c.universe._

  def atStaticImpl[T](implicit T: WeakTypeTag[T]): Expr[Resource] = {
    val rtc = Expr[Class[_]] {
      try {
        c.reifyRuntimeClass(T.tpe, concrete = true)
      } catch {
        case _: ReificationException => c.abort(c.enclosingPosition, s"${T.tpe} is not a concrete type")
      }
    }
    atDynamicImpl(rtc)
  }

  def atDynamicImpl(clazz: Expr[Class[_]]): Expr[Resource] =
    reify {
      new Resource {
        override def url(name: String) = Option(clazz.splice.getResource(name))
      }
    }

  def myImpl: Expr[Resource] = {
    val rtc = c.reifyEnclosingRuntimeClass
    if (rtc.isEmpty) {
      // The documentation for reifyEnclosingRuntimeClass claims that this is somehow possible!?
      c.abort(c.enclosingPosition, "this location doesn't correspond to a Java class file")
    }
    atDynamicImpl(Expr[Class[_]](rtc))
  }
}
