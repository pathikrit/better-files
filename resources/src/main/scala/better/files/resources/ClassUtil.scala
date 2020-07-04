/*
 * Copyright 2014 Frugal Mechanic (http://frugalmechanic.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package better.files.resources

import java.lang.annotation.Annotation
import java.lang.reflect.{Method, Modifier}
import java.net.{JarURLConnection, URLConnection, URLDecoder}
import java.io.{File, InputStream}
import java.nio.file.Path
import java.util.jar.{JarEntry, JarFile}
import scala.collection.JavaConverters._
import scala.reflect.{ClassTag, classTag}
import com.typesafe.scalalogging.LazyLogging
import java.time.Instant

/**
  * This contains utility methods for scanning Classes or Files on the classpath.
  *
  * Originally we used the classpath scanning functionality in the Spring Framework
  * and then later switched to the Reflections library (https://code.google.com/p/reflections/)
  * to avoid the dependency on Spring.  At some point we ran into issues with the Reflections
  * library not properly detecting classes so I ended up writing this as a replacement.
  */
object ClassUtil extends LazyLogging {

  // Note: The classpath separator is *ALWAYS* a / and should not be File.separator
  // See:
  //   https://www.atlassian.com/blog/archives/how_to_use_file_separator_when
  //   http://docs.oracle.com/javase/8/docs/api/java/lang/ClassLoader.html#getResource-java.lang.String-
  private def classpathSeparator: String = "/"

  def classForName(cls: String): Class[_]                           = classForName(cls, defaultClassLoader)
  def classForName(cls: String, classLoader: ClassLoader): Class[_] = Class.forName(cls, true, classLoader)

  def getClassForName(cls: String): Option[Class[_]] = getClassForName(cls, defaultClassLoader)

  def getClassForName(cls: String, classLoader: ClassLoader): Option[Class[_]] = {
    try {
      Option(Class.forName(cls, true, classLoader))
    } catch {
      case _: ClassNotFoundException => None
    }
  }

  def companionObject(cls: Class[_]): AnyRef = companionObjectAs[AnyRef](cls)

  def companionObjectAs[T <: AnyRef: ClassTag](cls: Class[_]): T = {
    companionObjectAs(cls, classTag[T].runtimeClass.asInstanceOf[Class[T]])
  }

  def companionObjectAs[T <: AnyRef](cls: Class[_], asCls: Class[T]): T = {
    val objectCls: Class[_] = companionObjectClass(cls)
    require(asCls.isAssignableFrom(objectCls), s"objectCls: $objectCls is not a asCls: $asCls")
    scalaObject(objectCls).asInstanceOf[T]
  }

  def getCompanionObject(cls: Class[_]): Option[AnyRef] = getCompanionObjectAs[AnyRef](cls)

  def getCompanionObjectAs[T <: AnyRef: ClassTag](cls: Class[_]): Option[T] = {
    getCompanionObjectAs(cls, classTag[T].runtimeClass.asInstanceOf[Class[T]])
  }

  def getCompanionObjectAs[T <: AnyRef](cls: Class[_], asCls: Class[T]): Option[T] = {
    val objectCls: Option[Class[_]] = getCompanionObjectClass(cls)

    if (objectCls.isEmpty || !asCls.isAssignableFrom(objectCls.get)) None
    else getScalaObjectAs(objectCls.get, asCls)
  }

  /**
    * Lookup the companion object class for a class
    */
  def companionObjectClass(cls: String): Class[_] = {
    companionObjectClass(cls, defaultClassLoader)
  }

  /**
    * Lookup the companion object class for a class
    */
  def companionObjectClass(cls: String, classLoader: ClassLoader): Class[_] = {
    getCompanionObjectClass(cls, classLoader).getOrElse { throw new ClassNotFoundException(s"No companion object class for $cls") }
  }

  /**
    * Lookup the companion object class for a class
    */
  def companionObjectClass(cls: Class[_]): Class[_] = {
    getCompanionObjectClass(cls).getOrElse { throw new ClassNotFoundException(s"No companion object class for $cls") }
  }

  /**
    * Lookup the companion object class for a class
    */
  def getCompanionObjectClass(cls: String): Option[Class[_]] = {
    getCompanionObjectClass(cls, defaultClassLoader)
  }

  /**
    * Lookup the companion object class for a class
    */
  def getCompanionObjectClass(cls: String, classLoader: ClassLoader): Option[Class[_]] = {
    getClassForName(cls, classLoader).flatMap { getCompanionObjectClass }
  }

  def getCompanionObjectClass(cls: Class[_]): Option[Class[_]] = {
    if (isScalaObject(cls)) Some(cls)
    else if (!cls.getName.endsWith("$")) getCompanionObjectClass(cls.getName + "$", cls.getClassLoader)
    else None
  }

  /**
    * Does this class represent a Scala object
    * @param cls The fully qualified name of the class to check (Note: should end with a '$' character)
    * @return
    */
  def isScalaObject(cls: String): Boolean = {
    isScalaObject(cls, defaultClassLoader)
  }

  /**
    * Does this class represent a Scala object
    * @param cls The fully qualified name of the class to check (Note: should end with a '$' character)
    * @return
    */
  def isScalaObject(cls: String, classLoader: ClassLoader): Boolean = {
    try {
      val c: Class[_] = classForName(cls, classLoader)
      isScalaObject(c)
    } catch {
      case _: ClassNotFoundException => false // Class does not exist
    }
  }

  /**
    * Is this the class for a Scala Object?
    */
  def isScalaObject(cls: Class[_]): Boolean = {
    try {
      cls.getField("MODULE$")
      true
    } catch {
      case _: NoSuchFieldException => false
    }
  }

  /**
    * Returns the Scala object instance for this class
    */
  def scalaObject(objectCls: Class[_]): AnyRef = scalaObjectAs[AnyRef](objectCls)

  /**
    * Returns the Scala object instance for this class
    */
  def scalaObjectAs[T <: AnyRef: ClassTag](objectCls: Class[_]): T = {
    scalaObjectAs(objectCls, classTag[T].runtimeClass.asInstanceOf[Class[T]])
  }

  /**
    * Returns the Scala object instance for this class
    */
  def scalaObjectAs[T <: AnyRef](objectCls: Class[_], asCls: Class[T]): T = {
    require(asCls.isAssignableFrom(objectCls), s"objectCls: $objectCls is not a asCls: $asCls")
    objectCls.getField("MODULE$").get(objectCls).asInstanceOf[T]
  }

  /**
    * Returns the Scala object instance for this class (if it is the class of a Scala object)
    */
  def getScalaObject(objectCls: Class[_]): Option[AnyRef] = getScalaObjectAs[AnyRef](objectCls)

  /**
    * Returns the Scala object instance for this class (if it is the class of a Scala object)
    */
  def getScalaObjectAs[T <: AnyRef: ClassTag](objectCls: Class[_]): Option[T] = {
    getScalaObjectAs(objectCls, classTag[T].runtimeClass.asInstanceOf[Class[T]])
  }

  /**
    * Returns the Scala object instance for this class (if it is the class of a Scala object)
    */
  def getScalaObjectAs[T <: AnyRef](objectCls: Class[_], asCls: Class[T]): Option[T] = {
    try {
      val res: AnyRef = objectCls.getField("MODULE$").get(objectCls)
      if (res.isNotNull && asCls.isAssignableFrom(res.getClass)) Some(res.asInstanceOf[T]) else None
    } catch {
      case _: NoSuchFieldException   => None // No MODULE$ field.  Not a Scala object?
      case _: ClassNotFoundException => None // Object does not exist
      case _: ClassCastException     => None // Object is not an instance of T
    }
  }

  /**
    * Can an instance of this class be created using a zero-args constructor?
    */
  def canCreateInstanceOf(cls: Class[_]): Boolean = {
    try {
      cls.getDeclaredConstructor()
      true
    } catch {
      case _: NoSuchMethodException => false
    }
  }

  /**
    * Can an instance of this class be created using a zero-args constructor?
    */
  def canCreateInstanceOfOrIsObject(cls: Class[_]): Boolean = {
    if (isScalaObject(cls)) true
    else canCreateInstanceOf(cls)
  }

  /**
    * Creates a new instance of a class using a 0-args constructor or returns the Scala object instance of this class
    */
  def newInstanceOrObject[T <: AnyRef](cls: Class[T]): T = {
    if (isScalaObject(cls)) scalaObjectAs(cls, cls)
    else cls.getDeclaredConstructor().newInstance()
  }

  /**
    * Creates a new instance of a class using a 0-args constructor or returns the Scala object instance of this class
    */
  def newInstanceOrObjectAs[T <: AnyRef: ClassTag](cls: Class[_]): T = {
    newInstanceOrObjectAs(cls, classTag[T].runtimeClass.asInstanceOf[Class[T]])
  }

  /**
    * Creates a new instance of a class using a 0-args constructor or returns the Scala object instance of this class
    */
  def newInstanceOrObjectAs[T <: AnyRef](cls: Class[_], asCls: Class[T]): T = {
    require(asCls.isAssignableFrom(cls), s"cls: $cls is not a asCls: $asCls")

    if (isScalaObject(cls)) scalaObjectAs(cls, asCls)
    else cls.getDeclaredConstructor().newInstance().asInstanceOf[T]
  }

  /**
    * Creates a new instance of a class using a 0-args constructor or returns the Scala object instance of this class
    */
  def getNewInstanceOrObject[T <: AnyRef](cls: Class[T]): Option[T] = {
    getNewInstanceOrObjectAs(cls, cls)
  }

  def getNewInstanceOrObjectAs[T <: AnyRef: ClassTag](cls: Class[_]): Option[T] = {
    getNewInstanceOrObjectAs(cls, classTag[T].runtimeClass.asInstanceOf[Class[T]])
  }

  /**
    * Creates a new instance of a class using a 0-args constructor or returns the Scala object instance of this class
    * @param cls The class to create an instance of (or to get the Object instance for)
    * @param asCls The return type
    * @tparam T
    * @return
    */
  def getNewInstanceOrObjectAs[T <: AnyRef](cls: Class[_], asCls: Class[T]): Option[T] = {
    if (isScalaObject(cls)) {
      getScalaObjectAs(cls, asCls)
    } else {
      try {
        if (!asCls.isAssignableFrom(cls)) None
        else Option(cls.getDeclaredConstructor().newInstance().asInstanceOf[T])
      } catch {
        case _: IllegalAccessException => None // Private Constructor
        case _: InstantiationException => None // No 0-args Constructor
        case _: NoSuchMethodException  => None // No 0-args Constructor
      }
    }
  }

  /**
    * Check if a class is loaded
    */
  def isClassLoaded(cls: String): Boolean = isClassLoaded(cls, defaultClassLoader)

  /**
    * Check if a class is loaded
    */
  def isClassLoaded(cls: String, classLoader: ClassLoader): Boolean = findLoadedClass(cls, classLoader).isDefined

  def findLoadedClass(cls: String): Option[Class[_]] = findLoadedClass(cls, defaultClassLoader)

  def findLoadedClass(cls: String, classLoader: ClassLoader): Option[Class[_]] = {
    val findLoadedClass: Method = classOf[ClassLoader].getDeclaredMethod("findLoadedClass", classOf[String])
    findLoadedClass.setAccessible(true)
    val res: Object = findLoadedClass.invoke(classLoader, cls)
    if (null == res) None else Some(res.asInstanceOf[Class[_]])
  }

  /**
    * Check if a class exists.
    */
  def classExists(cls: String): Boolean = classExists(cls, defaultClassLoader)

  /**
    * Check if a class exists.
    */
  def classExists(cls: String, classLoader: ClassLoader): Boolean =
    try {
      classLoader.loadClass(cls)
      true
    } catch {
      case _: ClassNotFoundException => false
    }

  /** Check if a file exists on the classpath */
  def classpathFileExists(file: String): Boolean = classpathFileExists(file, defaultClassLoader)

  /** Check if a file exists on the classpath */
  def classpathFileExists(file: String, classLoader: ClassLoader): Boolean = {
    classpathFileExists(new File(file), classLoader)
  }

  /** Check if a file exists on the classpath */
  def classpathFileExists(file: File): Boolean = classpathFileExists(file, defaultClassLoader)

  /** Check if a file exists on the classpath */
  def classpathFileExists(file: File, classLoader: ClassLoader): Boolean = {
    withClasspathURL(file, classLoader) { url: URL =>
      if (url.isFile) url.toFile.isRegularFile
      else
        withURLInputStream(url) { is: InputStream =>
          // This should work for a file
          try { is.read(); true }
          catch { case ex: NullPointerException => false }
        }
    }.getOrElse(false)
  }

  /** Check if a directory exists on the classpath */
  def classpathDirExists(file: String): Boolean = classpathDirExists(file, defaultClassLoader)

  /** Check if a directory exists on the classpath */
  def classpathDirExists(file: String, classLoader: ClassLoader): Boolean = {
    classpathDirExists(new File(file), classLoader)
  }

  /** Check if a directory exists on the classpath */
  def classpathDirExists(file: File): Boolean = classpathDirExists(file, defaultClassLoader)

  /** Check if a directory exists on the classpath */
  def classpathDirExists(file: File, classLoader: ClassLoader): Boolean = {
    withClasspathURL(file, classLoader) { url: URL =>
      if (url.isFile) url.toFile.isDirectory()
      else
        withURLInputStream(url) { is: InputStream =>
          // Not sure if there is a better way to do this -- A NullPointerException is thrown for a directory
          try { is.read(); false }
          catch { case ex: Exception => true }
        }
    }.getOrElse(false)
  }

  /** Lookup the lastModified timestamp for a resource on the classpath */
  def classpathLastModified(file: String): Long = classpathLastModified(file, defaultClassLoader)

  /** Lookup the lastModified timestamp for a resource on the classpath */
  def classpathLastModified(file: String, classLoader: ClassLoader): Long = {
    classpathLastModified(new File(file), classLoader)
  }

  /** Lookup the lastModified timestamp for a resource on the classpath */
  def classpathLastModified(file: File): Long = classpathLastModified(file, defaultClassLoader)

  /** Lookup the lastModified timestamp for a resource on the classpath */
  def classpathLastModified(file: File, classLoader: ClassLoader): Long = {
    withClasspathURLConnection(file, classLoader) { conn: URLConnection =>
      conn match {
        case j: JarURLConnection if null != j.getJarEntry => j.getJarEntry.getLastModifiedTime.toMillis
        case _                                            => conn.getLastModified
      }
    }.getOrElse(0L) // This default matches File.lastModified()
  }

  /** Lookup the legnth for a resource on the classpath */
  def classpathContentLength(file: String): Long = classpathContentLength(file, defaultClassLoader)

  /** Lookup the legnth for a resource on the classpath */
  def classpathContentLength(file: String, classLoader: ClassLoader): Long = {
    classpathContentLength(new File(file), classLoader)
  }

  /** Lookup the legnth for a resource on the classpath */
  def classpathContentLength(file: File): Long = classpathContentLength(file, defaultClassLoader)

  /** Lookup the legnth for a resource on the classpath */
  def classpathContentLength(file: File, classLoader: ClassLoader): Long = {
    withClasspathURLConnection(file, classLoader) { _.getContentLengthLong() }.getOrElse(0L) // This default matches File.length()
  }

  /** A helper for the above methods */
  private def withClasspathURL[T](file: File, classLoader: ClassLoader)(f: URL => T): Option[T] = {
    val path: String      = file.toString.replace(File.pathSeparatorChar, '/').stripPrefix(classpathSeparator)
    val urls: Vector[URL] = classLoader.getResources(path).asScala.toVector

    urls.headOption.map { url: URL => f(url) }
  }

  /** A helper for the above methods */
  private def withClasspathURLConnection[T](file: File, classLoader: ClassLoader)(f: URLConnection => T): Option[T] = {
    withClasspathURL(file, classLoader) { url: URL =>
      val conn: URLConnection = url.openConnection()
      f(conn)
    }
  }

  /** A helper for the above methods */
  private def withURLInputStream[T](url: URL)(f: InputStream => T): T = {
    val is: InputStream = url.openStream()
    try {
      f(is)
    } finally {
      // close() can throw exceptions if the file doesn't exist
      try { is.close() }
      catch { case _: Exception => }
    }
  }

  /**
    * Check if a class exists.  If it does not then a ClassNotFoundException is thrown.
    */
  def requireClass(cls: String, msg: => String): Unit = requireClass(cls, msg, defaultClassLoader)

  /**
    * Check if a class exists.  If it does not then a ClassNotFoundException is thrown.
    */
  def requireClass(cls: String, msg: => String, classLoader: ClassLoader): Unit = {
    if (!classExists(cls, classLoader)) throw new ClassNotFoundException(s"Missing Class: $cls - $msg")
  }

  /**
    * Find all classes annotated with a Java Annotation.
    *
   * Note: This loads ALL classes under the basePackage!
    */
  def findAnnotatedClasses[T <: Annotation](basePackage: String, annotationClass: Class[T]): Set[Class[_]] = {
    findAnnotatedClasses(basePackage, annotationClass, defaultClassLoader)
  }

  /**
    * Find all classes annotated with a Java Annotation.
    *
    * Note: This loads ALL classes under the basePackage!
    */
  def findAnnotatedClasses[T <: Annotation](basePackage: String, annotationClass: Class[T], classLoader: ClassLoader): Set[Class[_]] = {
    findClassNames(basePackage, classLoader) /*.filterNot { _.contains("$") }*/.map { classLoader.loadClass }.filter { c: Class[_] =>
      c.getAnnotation(annotationClass) != null
    }
  }

  /**
    * Finds all Scala Objects that extends a trait/interface/class.
    *
   * Note: This loads ALL classes under the basePackage and uses Class.isAssignableFrom for checking.
    */
  def findImplementingObjects[T <: AnyRef](basePackage: String, clazz: Class[T]): Set[T] = {
    findImplementingObjects(basePackage, clazz, defaultClassLoader)

  }

  /**
    * Finds all Scala Objects that extends a trait/interface/class.
    *
   * Note: This loads ALL classes under the basePackage and uses Class.isAssignableFrom for checking.
    */
  def findImplementingObjects[T <: AnyRef](basePackage: String, clazz: Class[T], classLoader: ClassLoader): Set[T] = {
    findImplementingClasses(basePackage, clazz, classLoader).filter { isScalaObject }.map { scalaObjectAs(_, clazz) }
  }

  /**
    * Find all concrete classes that extend a trait/interface/class.
    *
   * Note: This loads ALL classes under the basePackage and uses Class.isAssignableFrom for checking.
    */
  def findImplementingClasses[T](basePackage: String, clazz: Class[T]): Set[Class[_ <: T]] = {
    findImplementingClasses[T](basePackage, clazz, defaultClassLoader)
  }

  /**
    * Find all concrete classes that extend a trait/interface/class.
    *
    * Note: This loads ALL classes under the basePackage and uses Class.isAssignableFrom for checking.
    */
  def findImplementingClasses[T](basePackage: String, clazz: Class[T], classLoader: ClassLoader): Set[Class[_ <: T]] = {
    findClassNames(basePackage, classLoader) /*.filterNot{ _.contains("$") }*/
      .map { classLoader.loadClass }
      .filter { c: Class[_] =>
        clazz.isAssignableFrom(c)
      }
      .filterNot { c: Class[_] =>
        val mods: Int = c.getModifiers()
        Modifier.isAbstract(mods) || Modifier.isInterface(mods)
      }
      .map { _.asInstanceOf[Class[_ <: T]] }
  }

  /**
    * Find all class names under the base package (includes anonymous/inner/objects etc...)
    */
  def findClassNames(basePackage: String): Set[String] = findClassNames(basePackage, defaultClassLoader)

  /**
    * Find all class names under the base package (includes anonymous/inner/objects etc...)
    */
  def findClassNames(basePackage: String, classLoader: ClassLoader): Set[String] = {
    findClasspathFiles(basePackage, classLoader)
      .filter { f: File =>
        f.getName.endsWith(".class")
      }
      .map { f: File =>
        val name: String = f.toString()
        name.substring(0, name.length - ".class".length).replace(File.separator, ".")
      }
  }

  /**
    * Similar to File.listFiles() (i.e. a non-recursive findClassPathFiles)
    */
  def listClasspathFiles(basePackage: String): Set[File] = listClasspathFiles(basePackage, defaultClassLoader)

  /**
    * Similar to File.listFiles() (i.e. a non-recursive findClassPathFiles)
    */
  def listClasspathFiles(basePackage: String, classLoader: ClassLoader): Set[File] = {
    val packageDirPath: Path = new File(getPackageDirPath(basePackage)).toPath

    // An empty Path("") will still return 1 for packageDirPath.getNameCount(), which will lead to an exception
    // You can technically have a directory named " ", so using .isEmpty and not .isNullOrBlank
    val subPathLength: Int = if (packageDirPath.toString.isEmpty) 1 else packageDirPath.getNameCount() + 1

    findClasspathFiles(basePackage, classLoader).map { _.toPath.subpath(0, subPathLength).toFile }
  }

  /**
    * Recursively Find files on the classpath given a base package.
    */
  def findClasspathFiles(basePackage: String): Set[File] = findClasspathFiles(basePackage, defaultClassLoader)

  /**
    * Recursively Find files on the classpath given a base package.
    */
  def findClasspathFiles(basePackage: String, classLoader: ClassLoader): Set[File] = {
    val packageDirPath: String = getPackageDirPath(basePackage)
    val urls: Set[URL]         = classLoader.getResources(packageDirPath).asScala.toSet

    urls.flatMap { url: URL =>
      val filePath: String = URLDecoder.decode(url.getFile(), "UTF-8")

      url.getProtocol() match {
        case "jar" =>
          val jarFile: String   = filePath.substring("file:".length, filePath.indexOf("!"))
          val jarPrefix: String = filePath.substring(filePath.indexOf("!") + 1)
          require(
            jarPrefix == classpathSeparator + packageDirPath,
            s"Expected jarPrefix ($jarPrefix) to equal package prefix ($classpathSeparator$packageDirPath)"
          )
          scanJar(packageDirPath + classpathSeparator, new File(jarFile))

        case "file" =>
          val packageDir: File = new File(filePath)
          if (packageDir.isDirectory) recursiveListFiles(packageDir).map { f: File => packageDir.toPath.relativize(f.toPath).toFile }.map {
            f: File => new File(packageDirPath, f.toString)
          }
          else Nil

        case _ =>
          logger.warn("Unknown classpath entry: " + url)
          Nil
      }
    }
  }

  private def recursiveListFiles(dir: File): Set[File] = {
    require(dir.isDirectory, s"Expected file to be a directory: $dir")
    dir.listFiles.flatMap { f: File =>
      if (f.isFile) List(f)
      else if (f.isDirectory) recursiveListFiles(f)
      else Nil
    }.toSet
  }

  private def scanJar(prefix: String, jarFile: File): Set[File] = {
    require(jarFile.exists, s"Missing jar file: $jarFile")
    if (prefix != "") {
      require(!prefix.startsWith(classpathSeparator), s"Prefix should not starts with $classpathSeparator")
      require(prefix.endsWith(classpathSeparator), s"Non-Empty prefix should end with $classpathSeparator")
    }

    val builder = Set.newBuilder[File]

    Resource.using(new JarFile(jarFile)) { jar: JarFile =>
      jar.entries().asScala.foreach { entry: JarEntry =>
        val name: String = entry.getName
        if (!entry.isDirectory && name.startsWith(prefix)) builder += new File(name)
      }
    }

    builder.result
  }

  private def getPackageDirPath(basePackage: String): String = {
    basePackage.stripPrefix(classpathSeparator).replace(".", classpathSeparator)
  }

  private def defaultClassLoader: ClassLoader = {
    val cl: ClassLoader = Thread.currentThread.getContextClassLoader
    if (null != cl) cl else getClass().getClassLoader()
  }
}
