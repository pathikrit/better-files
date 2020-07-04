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

package better.files.resources.rich

import better.files.resources.{ClassUtil, toRichPath, RichString}
import java.io.File
import java.time.Instant
import scala.collection.mutable.Builder

final class RichFile(val f: File) extends AnyVal {

  // getResource[...](path) always uses "/" for separator - https://docs.oracle.com/javase/8/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String)
  def toResourcePath: String = f.toString.replace(File.separatorChar, '/')

  def isClasspathFile: Boolean                  = ClassUtil.classpathFileExists(f)
  def isClasspathFile(cl: ClassLoader): Boolean = ClassUtil.classpathFileExists(f, cl)

  def isClasspathDirectory: Boolean                  = ClassUtil.classpathDirExists(f)
  def isClasspathDirectory(cl: ClassLoader): Boolean = ClassUtil.classpathDirExists(f, cl)

  def classpathLastModified: Long                  = ClassUtil.classpathLastModified(f)
  def classpathLastModified(cl: ClassLoader): Long = ClassUtil.classpathLastModified(f, cl)

  def classpathLength: Long                  = ClassUtil.classpathContentLength(f)
  def classpathLength(cl: ClassLoader): Long = ClassUtil.classpathContentLength(f, cl)

  /**
    * The extension (if any) of this file
    */
  def extension: Option[String] = {
    // July 30 2014 - The file should not have to be a file for this to work
    //require(f.isFile, s"Not a file: $f")
    val name: String    = f.getName()
    val indexOfDot: Int = name.lastIndexOf('.')
    if (-1 == indexOfDot) None else Some(name.substring(indexOfDot + 1))
  }

  /**
    * The name of the file without it's extension
    */
  def nameWithoutExtension: String = {
    // July 30 2014 - The file should not have to be a file for this to work
    //require(f.isFile, s"Not a file: $f")
    val name: String    = f.getName()
    val indexOfDot: Int = name.lastIndexOf('.')
    if (-1 == indexOfDot) name else name.substring(0, indexOfDot)
  }

  /**
    * Change or Add an extension to this file
    */
  def withExtension(ext: String): File = {
    new File(f.getParent(), nameWithoutExtension + ext.requireLeading("."))
  }

  /** If this path starts with the passed in path then strip it */
  def stripLeading(path: File): File = f.toPath.stripPrefix(path.toPath).toFile

  /** If this path ends with the passed in path then strip it */
  def stripTrailing(path: File): File = f.toPath.stripSuffix(path.toPath).toFile

  /**
    * Find all files under this directory (directories are not included in the result)
    *
    * Deprecated due to ambiguity.  Prefer the fm.lazyseq.Implicits.RichLazySeqFile implementation
    */
  @Deprecated
  def findFiles(recursive: Boolean = true): Vector[File] = {
    val builder: Builder[File, Vector[File]] = Vector.newBuilder[File]
    findFiles0(f, recursive, builder)
    builder.result
  }

  private def findFiles0(dir: File, recursive: Boolean, builder: Builder[File, Vector[File]]): Unit = {
    require(dir.isDirectory, s"Not a directory: $dir")

    val children: Array[File] = dir.listFiles()

    if (null != children) children.foreach { child: File =>
      if (child.isDirectory && recursive) findFiles0(child, recursive, builder)
      else if (child.isFile) builder += child
    }
  }

  /**
    * The File.lastModified value truncated to seconds (which matches the pre JDK-9 Linux/OSX behavior of File.lastModified)
    *
   * @return The number of milliseconds since since the epoch (00:00:00 GMT, January 1, 1970) truncated to second precision.
    */
  def lastModifiedWithSecondPrecision: Long = {
    val millis: Long = f.lastModified()
    if (0L == millis) return 0L

    millis - (millis % 1000L)
  }
}
