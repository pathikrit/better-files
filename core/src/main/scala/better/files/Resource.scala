package better.files

import java.io.InputStream
import java.net.URL

import com.github.ghik.silencer.silent

import scala.annotation.compileTimeOnly
import scala.reflect.macros.{ReificationException, blackbox}

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

  /*
   * Ordinarily, we'd have a trait ResourceLookup with apply/url/asFile methods, make object Resource be the default implementation (using the context class loader), and make “modifiers” like `at` and `my` return different ResourceLookup implementations (with suitable lookup behavior).
   *
   * But we're using macros here. Macros cannot have run-time polymorphism like that, because they run at compile time! Instead, we make the modifier methods stubs (they just return this), and the macros look whether they were called on the object returned by a modifier method (even though it's just this).
   *
   * It's a hack, but it works, and the resulting API is as easy to use as the idiomatic trait-and-instances approach.
   */

  /**
    * Look up class resource files.
    *
    * When this method is part of a call to the lookup methods `apply`, `url`, and `asFile`, it changes how they look up resource files. The `name` parameter to those methods is instead interpreted as a path relative to the JVM class file for `T`. In other words, they look up resources using [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]] and [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResourceAsStream(java.lang.String) Class#getResourceAsStream]].
    *
    * For example, if `com.example.ExampleClass` is given for `T`, then the resource file will be searched for in the `com/example` folder containing `ExampleClass.class`.
    *
    * If you want to look up resource files relative to the call site instead (that is, you want a class to look up one of its own resources), use the `my` method instead.
    *
    * @example {{{ Resource.at[YourClass].asFile("config.properties") }}}
    * @note This method, if used, must be a direct part of a call to the aforementioned lookup methods. If used in any other way, there will be a compile error. For example, this will not work:
    *       {{{
    *       val at = Resource.at[SomeClass]
    *       at.asFile("some-file.txt")
    *       }}}
    *
    *       The reason for this is that this method exists only to be seen by the macros implementing the lookup methods. The macros look whether this method was called, and change their behavior accordingly. If called by run-time reflection, this method does nothing.
    * @tparam T The class to look up from.
    * @return This object. Call apply, url, or asFile on the returned object.
    */
  // These are stub methods, so ignore warnings about their parameters being unused.
  @compileTimeOnly("you must directly call apply, url, or asFile on the returned object")
  def at[@silent T]: this.type = this

  /**
    * Look up class resource files.
    *
    * When this method is part of a call to the lookup methods `apply`, `url`, and `asFile`, it changes how they look up resource files. The `name` parameter to those methods is instead interpreted as a path relative to the given `lookupClass`. In other words, they look up resources using [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]] and [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResourceAsStream(java.lang.String) Class#getResourceAsStream]].
    *
    * For example, if `classOf[com.example.ExampleClass]` is given, then the resource file will be searched for in the `com/example` folder containing `ExampleClass.class`.
    *
    * If you want to look up resource files relative to the call site instead (that is, you want a class to look up one of its own resources), use the `my` method instead.
    *
    * @example {{{ Resource.at(Class.forName("your.AppClass")).asFile("config.properties") }}}
    * @note This method, if used, must be a direct part of a call to the aforementioned lookup methods. If used in any other way, there will be a compile error. For example, this will not work:
    *       {{{
    *       val at = Resource.at(someClass)
    *       at.asFile("some-file.txt")
    *       }}}
    *
    *       The reason for this is that this method exists only to be seen by the macros implementing the lookup methods. The macros look whether this method was called, and change their behavior accordingly. If called by run-time reflection, this method does nothing.
    * @param lookupClass The class to look up from.
    * @return This object. Call apply, url, or asFile on the returned object.
    */
  @compileTimeOnly("you must directly call apply, url, or asFile on the returned object")
  def at(@silent lookupClass: Class[_]): this.type = this

  /**
    * Look up own resource files.
    *
    * When this method is part of a call to the lookup methods `apply`, `url`, and `asFile`, it changes how they look up resource files. The `name` parameter to those methods is instead interpreted as a path relative to the JVM class file for the call site. In other words, they look up resources using [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]] and [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResourceAsStream(java.lang.String) Class#getResourceAsStream]].
    *
    * For example, if the call to this method appears inside the class `com.example.ExampleClass`, then the resource file will be searched for in the `com/example` folder containing `ExampleClass.class`.
    *
    * @example {{{ Resource.my.asFile("config.properties") }}}
    * @note This method, if used, must be a direct part of a call to the aforementioned lookup methods. If used in any other way, there will be a compile error. For example, this will not work:
    *       {{{
    *       val my = Resource.my
    *       my.asFile("some-file.txt")
    *       }}}
    *
    *       The reason for this is that this method exists only to be seen by the macros implementing the lookup methods. The macros look whether this method was called, and change their behavior accordingly. If called by run-time reflection, this method does nothing.
    * @return This object. Call apply, url, or asFile on the returned object.
    */
  @compileTimeOnly("you must directly call apply, url, or asFile on the returned object")
  def my: this.type = this

  /**
    * Look up resource files using the specified ClassLoader.
    *
    * When this method is part of a call to the lookup methods `apply`, `url`, and `asFile`, it changes which ClassLoader they use to look up resource files. By default, it's the current thread's context class loader, but using this method designates another ClassLoader to use instead.
    *
    * @example {{{ Resource.from(appClassLoader).asFile("your/config.properties") }}}
    * @note This method, if used, must be a direct part of a call to the aforementioned lookup methods. If used in any other way, there will be a compile error. For example, this will not work:
    *       {{{
    *       val from = Resource.from(appClassLoader)
    *       from.asFile("some-file.txt")
    *       }}}
    *
    *       The reason for this is that this method exists only to be seen by the macros implementing the lookup methods. The macros look whether this method was called, and change their behavior accordingly. If called by run-time reflection, this method does nothing.
    * @return This object. Call apply, url, or asFile on the returned object.
    */
  @compileTimeOnly("you must directly call apply, url, or asFile on the returned object")
  def from(@silent cl: ClassLoader): this.type = this

  /**
    * Why do we need macros?
    * See: https://github.com/pathikrit/better-files/pull/227
    */
  private[this] class Macros(val c: blackbox.Context) {
    import c.universe._

    private[this] def lookupSource: Tree = {
      object CallToResource {
        def unapply(tree: Tree): Option[Tree] = {
          if (tree.symbol.owner == symbolOf[Resource.type]) Some(tree)
          else None
        }
      }

      (c.prefix.tree match {
        case CallToResource(q"$_.at[$t]") =>
          try c.reifyRuntimeClass(t.tpe, concrete = true)
          catch {
            case e: ReificationException =>
              c.abort(t.pos, s"$t is not a concrete type")
          }

        case CallToResource(q"$_.at($lookupClass)") => lookupClass
        case CallToResource(q"$_.from($cl)")        => cl

        case CallToResource(q"$_.my") =>
          c.reifyEnclosingRuntimeClass match {
            case EmptyTree =>
              // The documentation for reifyEnclosingRuntimeClass claims that this is possible, somehow. I have no idea where a macro call could possibly appear that's not inside a scope that compiles to a class file, but I guess we'll have to deal with it.
              c.abort(c.enclosingPosition,
                      "cannot use ‘my’ here, because this location doesn't correspond to a Java class file")
            case t => t
          }

        case _ => q"_root_.java.lang.Thread.currentThread.getContextClassLoader"
      }): @silent // scalac generates bogus unused pattern variable warnings here.
    }

    def asFileImpl(name: c.Expr[String]): Tree =
      q"_root_.better.files.File($lookupSource.getResource($name))"

    def applyImpl(name: c.Expr[String]): Tree =
      q"$lookupSource.getResourceAsStream($name)"

    def urlImpl(name: c.Expr[String]): Tree =
      q"$lookupSource.getResource($name)"
  }
}
