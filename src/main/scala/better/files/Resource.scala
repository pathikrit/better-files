package better.files

import java.io.{IOException, InputStream}
import java.net.URL
import java.nio.charset.Charset

/** Finds and loads [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) class resources]]
  * or [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) class loader resources]].
  *
  * The default implementation of this trait is the [[Resource]] object, which looks up resources
  * using the [[https://docs.oracle.com/javase/10/docs/api/java/lang/Thread.html#currentThread() current thread]]'s [[https://docs.oracle.com/javase/10/docs/api/java/lang/Thread.html#getContextClassLoader() context class loader]].
  * The Resource object also offers several other Resource implementations,
  * through its methods `at`, `from`, and `my`. `at` searches from a [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html Class]],
  * `from` searches from a [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html ClassLoader]],
  * and `my` searches from the class, trait, or object surrounding the call.
  *
  * @example {{{
  *          // Look up the config.properties file for this class or object.
  *          Resource.my.asStream("config.properties")
  *
  *          // Find logging.properties (in the root package) somewhere on the classpath.
  *          Resource.url("logging.properties")
  *          }}}
  *
  * @see [[Resource]]
  * @see [[https://stackoverflow.com/questions/676250/different-ways-of-loading-a-file-as-an-inputstream Different ways of loading a file as an InputStream]]
  * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
  * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) ClassLoader#getResource]]
  */
trait Resource {

  /** Look up a resource by name, and open an [[https://docs.oracle.com/javase/10/docs/api/java/io/InputStream.html InputStream]] for reading it.
    *
    * @param name Name of the resource to search for.
    * @return InputStream for reading the found resource, if a resource was found.
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResourceAsStream(java.lang.String) Class#getResourceAsStream]]
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResourceAsStream(java.lang.String) ClassLoader#getResourceAsStream]]
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
      bufferSize: Int = DefaultBufferSize,
      charset: Charset = DefaultCharset
  ): Option[String] =
    asStream(name).map(_.asString(bufferSize = bufferSize, charset = charset))

  def getAsString(
      name: String,
      bufferSize: Int = DefaultBufferSize,
      charset: Charset = DefaultCharset
  ): String =
    asString(name, bufferSize, charset).getOrElse(Resource.notFound(name))

  /** Look up a resource by name, and get its [[https://docs.oracle.com/javase/10/docs/api/java/net/URL.html URL]].
    *
    * @param name Name of the resource to search for.
    * @return URL of the requested resource. If the resource could not be found or is not accessible, returns None.
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) ClassLoader#getResource]]
    */
  def url(name: String): Option[URL]

  /** Get URL of given resource
    * A default argument of empty string is provided to conveniently get the root resource URL using {{Resource.getUrl()}}
    *
    * @param name
    * @return
    */
  def getUrl(name: String = ""): URL =
    url(name).getOrElse(Resource.notFound(name))
}

/** Implementations of [[Resource]].
  *
  * This object itself is a Resource uses the [[https://docs.oracle.com/javase/10/docs/api/java/lang/Thread.html#currentThread() current thread]]'s
  * [[https://docs.oracle.com/javase/10/docs/api/java/lang/Thread.html#getContextClassLoader() context class loader]].
  * It also creates Resources with different lookup behavior, using the methods `at`, `from`, and `my`. `at` searches
  * rom a [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html Class]], `from` searches
  * from a different [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html ClassLoader]],
  * and `my` searches from the class, trait, or object surrounding the call.
  *
  * @see [[Resource]]
  * @see [[https://stackoverflow.com/questions/676250/different-ways-of-loading-a-file-as-an-inputstream Different ways of loading a file as an InputStream]]
  * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
  * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) ClassLoader#getResource]]
  */
object Resource extends Resource with ResourceCompat {

  @throws[NoSuchElementException]
  def notFound(name: String): Nothing =
    throw new NoSuchElementException(s"Could not find resource=${name}")

  override def url(name: String): Option[URL] =
    from(Thread.currentThread.getContextClassLoader).url(name)

  /** Look up resource files using the specified ClassLoader.
    *
    * This Resource looks up resources from a specific ClassLoader. Like [[Resource the default Resource]], resource names are relative to the root package.
    *
    * @example {{{ Resource.from(appClassLoader).url("com/example/config.properties") }}}
    * @param cl ClassLoader to look up resources from.
    * @return A Resource that uses the supplied ClassLoader.
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) ClassLoader#getResource]]
    */
  def from(cl: ClassLoader): Resource =
    new Resource {
      override def url(name: String): Option[URL] =
        Option(cl.getResource(name))
    }
}
