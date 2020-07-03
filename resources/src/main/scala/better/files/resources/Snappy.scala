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

import java.io.{InputStream, OutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

object Snappy {
  private val HasSnappy: Boolean = ClassUtil.classExists("org.xerial.snappy.SnappyInputStream")
  private def requireSnappy(): Unit =
    if (!HasSnappy)
      throw new ClassNotFoundException(
        """Snappy support missing.  Please include snappy-java:  https://github.com/xerial/snappy-java   e.g.: libraryDependencies += "org.xerial.snappy" % "snappy-java" % "1.1.0.1""""
      )

  /**
    * Create a new SnappyOutputStream
    */
  def newOutputStream(os: OutputStream): OutputStream = {
    requireSnappy()
    Impl.newOS(os)
  }

  /**
    * Create a new SnappyInputStream
    */
  def newInputStream(is: InputStream): InputStream = {
    requireSnappy()
    Impl.newIS(is)
  }

  /**
    * If Snappy is available then create a new SnappyOutputStream otherwise use a GZIPOutputStream
    */
  def newSnappyOrGzipOutputStream(os: OutputStream): OutputStream = {
    if (HasSnappy) Impl.newOS(os) else new GZIPOutputStream(os)
  }

  /**
    * If Snappy is available then create a new SnappyInputStream otherwise use a GZIPInputStream
    */
  def newSnappyOrGzipInputStream(is: InputStream): InputStream = {
    if (HasSnappy) Impl.newIS(is) else new GZIPInputStream(is)
  }

  // This is a separate object to prevent NoClassDefFoundError
  private object Impl {
    import org.xerial.snappy.{SnappyInputStream, SnappyOutputStream}
    def newOS(os: OutputStream): OutputStream = new SnappyOutputStream(os)
    def newIS(is: InputStream): InputStream   = new SnappyInputStream(is)
  }
}
