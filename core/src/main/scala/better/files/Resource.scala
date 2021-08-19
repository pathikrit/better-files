package better.files

import java.io.{IOException, InputStream}
import java.net.URL
import java.nio.charset.Charset

import scala.language.experimental.macros
import scala.reflect.macros.{ReificationException, blackbox}

/** Finds and loads [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) class resources]] or
  * [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) class loader resources]].
  *
  * The default implementation of this trait is the [[Resource]] object, which looks up resources using the
  * [[https://docs.oracle.com/javase/10/docs/api/java/lang/Thread.html#currentThread() current thread]] 's
  * [[https://docs.oracle.com/javase/10/docs/api/java/lang/Thread.html#getContextClassLoader() context class loader]]. The Resource object
  * also offers several other Resource implementations, through its methods `at`, `from`, and `my`. `at` searches from a
  * [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html Class]], `from` searches from a
  * [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html ClassLoader]], and `my` searches from the class, trait, or
  * object surrounding the call.
  *
  * @example
  *   {{{ // Look up the config.properties file for this class or object. Resource.my.asStream("config.properties")
  *
  * // Find logging.properties (in the root package) somewhere on the classpath. Resource.url("logging.properties") }}}
  *
  * @see
  *   [[Resource]]
  * @see
  *   [[https://stackoverflow.com/questions/676250/different-ways-of-loading-a-file-as-an-inputstream Different ways of loading a file as an InputStream]]
  * @see
  *   [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
  * @see
  *   [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) ClassLoader#getResource]]
  */
trait Resource {

  /** Look up a resource by name, and open an [[https://docs.oracle.com/javase/10/docs/api/java/io/InputStream.html InputStream]] for
    * reading it.
    *
    * @param name
    *   Name of the resource to search for.
    * @return
    *   InputStream for reading the found resource, if a resource was found.
    * @see
    *   [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResourceAsStream(java.lang.String) Class#getResourceAsStream]]
    * @see
    *   [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResourceAsStream(java.lang.String) ClassLoader#getResourceAsStream]]
    */
  @throws[IOException]
  def asStream(name: String): Option[InputStream] =
    url(name).map(_.openStream())

  /** Same as asStream but throws a NoSuchElementException if resource is not found
    */
  def getAsStream(name: String): InputStream =
    asStream(name).getOrElse(Resource.notFound(name))

  def asString(
      name: String,
      bufferSize: Int = DefaultBufferSize
  )(implicit
      charset: Charset = DefaultCharset
  ): Option[String] =
    asStream(name).map(_.asString(bufferSize = bufferSize)(charset))

  def getAsString(
      name: String,
      bufferSize: Int = DefaultBufferSize
  )(implicit
      charset: Charset = DefaultCharset
  ): String =
    asString(name, bufferSize)(charset).getOrElse(Resource.notFound(name))

  /** Look up a resource by name, and get its [[https://docs.oracle.com/javase/10/docs/api/java/net/URL.html URL]].
    *
    * @param name
    *   Name of the resource to search for.
    * @return
    *   URL of the requested resource. If the resource could not be found or is not accessible, returns None.
    * @see
    *   [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
    * @see
    *   [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) ClassLoader#getResource]]
    */
  def url(name: String): Option[URL]

  /** Get URL of given resource A default argument of empty string is provided to conveniently get the root resource URL using
    * {{Resource.getUrl()}}
    *
    * @param name
    * @return
    */
  def getUrl(name: String = ""): URL =
    url(name).getOrElse(Resource.notFound(name))
}

/** Implementations of [[Resource]].
  *
  * This object itself is a Resource uses the
  * [[https://docs.oracle.com/javase/10/docs/api/java/lang/Thread.html#currentThread() current thread]] 's
  * [[https://docs.oracle.com/javase/10/docs/api/java/lang/Thread.html#getContextClassLoader() context class loader]]. It also creates
  * Resources with different lookup behavior, using the methods `at`, `from`, and `my`. `at` searches rom a
  * [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html Class]], `from` searches from a different
  * [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html ClassLoader]], and `my` searches from the class, trait, or
  * object surrounding the call.
  *
  * @see
  *   [[Resource]]
  * @see
  *   [[https://stackoverflow.com/questions/676250/different-ways-of-loading-a-file-as-an-inputstream Different ways of loading a file as an InputStream]]
  * @see
  *   [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
  * @see
  *   [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) ClassLoader#getResource]]
  */
object Resource extends Resource {

  @throws[NoSuchElementException]
  def notFound(name: String): Nothing =
    throw new NoSuchElementException(s"Could not find resource=${name}")

  override def url(name: String): Option[URL] =
    from(Thread.currentThread.getContextClassLoader).url(name)

  /** Look up class resource files.
    *
    * This Resource looks up resources relative to the JVM class file for `T`, using
    * [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]. For example, if
    * `com.example.ExampleClass` is given for `T`, then resource files will be searched for in the `com/example` folder containing
    * `ExampleClass.class`.
    *
    * If you want to look up resource files relative to the call site instead (that is, you want a class to look up one of its own
    * resources), use the `my` method instead.
    *
    * @example
    *   {{{Resource.at[YourClass].url("config.properties")}}}
    * @tparam T
    *   The class, trait, or object to look up from. Objects must be written with a `.type` suffix, such as `Resource.at[SomeObject.type]`.
    * @return
    *   A Resource for `T`.
    * @see
    *   [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
    */
  def at[T]: Resource =
    macro Macros.atStaticImpl[T]

  /** Look up class resource files.
    *
    * This Resource looks up resources from the given Class, using
    * [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]. For example, if
    * `classOf[com.example.ExampleClass]` is given for `clazz`, then resource files will be searched for in the `com/example` folder
    * containing `ExampleClass.class`.
    *
    * If you want to look up resource files relative to the call site instead (that is, you want your class to look up one of its own
    * resources), use the `my` method instead.
    *
    * @example
    *   {{{Resource.at(Class.forName("your.AppClass")).url("config.properties")}}}
    *
    * In this example, a file named `config.properties` is expected to appear alongside the file `AppClass.class` in the package `your`.
    * @param clazz
    *   The class to look up from.
    * @return
    *   A Resource for `clazz`.
    * @see
    *   [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
    */
  def at(clazz: Class[_]): Resource =
    macro Macros.atDynamicImpl

  /** Look up own resource files.
    *
    * This Resource looks up resources from the [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html Class]] surrounding the
    * call, using [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]. For
    * example, if `my` is called from `com.example.ExampleClass`, then resource files will be searched for in the `com/example` folder
    * containing `ExampleClass.class`.
    *
    * @example
    *   {{{Resource.my.url("config.properties")}}}
    * @return
    *   A Resource for the call site.
    * @see
    *   [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
    */
  def my: Resource =
    macro Macros.myImpl

  /** Look up resource files using the specified ClassLoader.
    *
    * This Resource looks up resources from a specific ClassLoader. Like [[Resource the default Resource]], resource names are relative to
    * the root package.
    *
    * @example
    *   {{{Resource.from(appClassLoader).url("com/example/config.properties")}}}
    * @param cl
    *   ClassLoader to look up resources from.
    * @return
    *   A Resource that uses the supplied ClassLoader.
    * @see
    *   [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) ClassLoader#getResource]]
    */
  def from(cl: ClassLoader): Resource =
    new Resource {
      override def url(name: String): Option[URL] =
        Option(cl.getResource(name))
    }

  /** Implementations of the `Resource.at` macros. This is needed because `Class#getResource` is caller-sensitive; calls to it must appear
    * in user code, ''not'' in better-files.
    */
  private[Resource] final class Macros(val c: blackbox.Context) {
    import c.universe._
    import c.Expr

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
}
