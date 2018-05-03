package better.files

import java.io.InputStream
import java.net.URL

import com.github.ghik.silencer.silent

import scala.annotation.compileTimeOnly
import scala.reflect.macros.{ReificationException, blackbox}

/**
  * Finds and loads [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) class loader resources]].
  *
  * The ''lookup methods'' `apply`, `url`, and `asFile` look up a resource, by name. They are different in what they return: `apply` returns an [[https://docs.oracle.com/javase/10/docs/api/java/io/InputStream.html InputStream]], `url` returns a [[https://docs.oracle.com/javase/10/docs/api/java/net/URL.html URL]], and `asFile` returns a [[File]].
  *
  * By default, resources are looked up using the [[https://docs.oracle.com/javase/10/docs/api/java/lang/Thread.html#currentThread() current thread]]'s [[https://docs.oracle.com/javase/10/docs/api/java/lang/Thread.html#getContextClassLoader() context class loader]]. The ''modifier methods'' `at`, `from`, and `my` change this: `at` searches from a [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html Class]], `from` searches from a [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html ClassLoader]], and `my` searches from the class, trait, or object surrounding the call.
  *
  * To use a modifier method, call it, then call a lookup method on the object returned by the modifier method. See the documentation of the modifier methods for details and examples.
  *
  * @example {{{
  *          // Get an InputStream for reading META-INF/example.txt.
  *          val in: InputStream = Resource("META-INF/example.txt")
  *
  *          // Look up app.conf from the package that the caller class
  *          // is in, and get a File representing it.
  *          val confFile: File = Resource.my.asFile("app.conf")
  *          }}}
  *
  * @see [[https://stackoverflow.com/questions/676250/different-ways-of-loading-a-file-as-an-inputstream Different ways of loading a file as an InputStream]]
  * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
  * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResourceAsStream(java.lang.String) Class#getResourceAsStream]]
  * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) ClassLoader#getResource]]
  * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResourceAsStream(java.lang.String) ClassLoader#getResourceAsStream]]
  *
  * @note As of Java SE 9, loading of class resources (using the `at` or `my` modifiers) is affected by which module is attempting to load them (that is, it's ''caller-sensitive''), in that the package that the resource is in must be [[https://docs.oracle.com/javase/10/docs/api/java/lang/Module.html#isOpen(java.lang.String,java.lang.Module) open]] to the calling module. For this reason, the lookup methods are implemented as macros, so that underlying calls to [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]] or [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResourceAsStream(java.lang.String) Class#getResourceAsStream]] appear in the caller's module, rather than in the module containing the better-files library.
  *
  * @groupname lookup Lookup Methods
  * @groupdesc lookup Find and load resources.
  * @groupprio lookup 1
  *
  * @groupname modifier Modifier Methods
  * @groupdesc modifier Select where the lookup methods search.
  * @groupprio modifier 2
  *
  * @define thisMethodCanBeModified By default, this method searches for the resource using the current thread's context class loader. This behavior can be changed using the ''modifier methods''. For details, please see the main documentation for [[Resource$ Resource]].
  */
object Resource {

  /**
    * Look up a resource by name, and open an [[https://docs.oracle.com/javase/10/docs/api/java/io/InputStream.html InputStream]] for reading it.
    *
    * $thisMethodCanBeModified
    *
    * @param name Name of the resource to search for.
    * @return InputStream for reading the found resource.
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResourceAsStream(java.lang.String) Class#getResourceAsStream]]
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResourceAsStream(java.lang.String) ClassLoader#getResourceAsStream]]
    * @group lookup
    */
  def apply(name: String): InputStream =
    macro Macros.applyImpl

  /**
    * Look up a resource by name, and get its [[https://docs.oracle.com/javase/10/docs/api/java/net/URL.html URL]].
    *
    * $thisMethodCanBeModified
    *
    * @param name Name of the resource to search for.
    * @return URL of the requested resource.
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) ClassLoader#getResource]]
    * @group lookup
    */
  def url(name: String): URL =
    macro Macros.urlImpl

  /**
    * Look up a resource by name, and get a [[File]] representing it.
    *
    * $thisMethodCanBeModified
    *
    * @param name Name of the resource to search for.
    * @return File representing the requested resource.
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) ClassLoader#getResource]]
    * @group lookup
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
    * @group modifier
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
    * @group modifier
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
    * @group modifier
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
    * @group modifier
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
