package better.files

import java.io.InputStream
import java.net.URL

import scala.reflect.ClassTag

case class Resource(classLoader: ClassLoader = ContextClassLoader()) {

  /**
    * Look up a resource by name, and open an [[https://docs.oracle.com/javase/10/docs/api/java/io/InputStream.html InputStream]] for reading it.
    *
    * @param name Name of the resource to search for.
    * @return InputStream for reading the found resource.
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResourceAsStream(java.lang.String) Class#getResourceAsStream]]
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResourceAsStream(java.lang.String) ClassLoader#getResourceAsStream]]
    */
  def apply(name: String): InputStream =
    classLoader.getResourceAsStream(name)

  /**
    * Look up a resource by name, and get its [[https://docs.oracle.com/javase/10/docs/api/java/net/URL.html URL]].
    *
    * @param name Name of the resource to search for.
    * @return URL of the requested resource.
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) ClassLoader#getResource]]
    */
  def url(name: String): URL =
    classLoader.getResource(name)

  /**
    * Look up a resource by name, and get a [[File]] representing it.
    *
    * @param name Name of the resource to search for.
    * @return File representing the requested resource.
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
    * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) ClassLoader#getResource]]
    */
  def asFile(name: String): File =
    File(url(name))
}

/**
  * Finds and loads [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) class loader resources]].
  *
  * By default, resources are looked up using the [[https://docs.oracle.com/javase/10/docs/api/java/lang/Thread.html#currentThread() current thread]]'s [[https://docs.oracle.com/javase/10/docs/api/java/lang/Thread.html#getContextClassLoader() context class loader]]. The ''modifier methods'' `at`, `from`, and `my` change this: `at` searches from a [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html Class]], `from` searches from a [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html ClassLoader]], and `my` searches from the class, trait, or object surrounding the call.
  *
  * @see [[https://stackoverflow.com/questions/676250/different-ways-of-loading-a-file-as-an-inputstream Different ways of loading a file as an InputStream]]
  * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
  * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResourceAsStream(java.lang.String) Class#getResourceAsStream]]
  * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String) ClassLoader#getResource]]
  * @see [[https://docs.oracle.com/javase/10/docs/api/java/lang/ClassLoader.html#getResourceAsStream(java.lang.String) ClassLoader#getResourceAsStream]]
  */
object Resource {

  def apply(name: String): InputStream =
    Resource()(name)

  def url(name: String): URL =
    Resource().url(name)

  def asFile(name: String): File =
    Resource().asFile(name)

  def at[A: ClassTag]: Resource =
    Resource(ContextClassLoader.of[A])

  def at(clazz: Class[_]): Resource =
    Resource(ContextClassLoader(clazz))
}
