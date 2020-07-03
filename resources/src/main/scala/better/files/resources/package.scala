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

package better.files

import scala.util.Try

package object resources extends Implicits with typesafeequals.Implicits {

  /** A type alias for java.net.URL */
  type URL = java.net.URL

  /** A type alias for java.net.URI */
  type URI = java.net.URI

  /**
    * Simple wrappers for the java.net.URL constructors
    */
  object URL {

    /**
      * Create a URL
      *
      * @param url The url
      * @return The URL instance
      */
    def apply(url: String): URL = new java.net.URL(url)

    /**
      * Try to create a URL
      *
      * @param url The url
      * @return A Try[URL]
      */
    def tryParse(url: String): Try[URL] = Try { apply(url) }

    /**
      * Same as URL.tryParse(url).toOption
      */
    def get(url: String): Option[URL] = tryParse(url).toOption
  }

  /**
    * Simple wrappers for the java.net.URI constructors
    */
  object URI {
    def apply(uri: String): URI         = new java.net.URI(uri)
    def tryParse(uri: String): Try[URI] = Try { apply(uri) }
    def get(uri: String): Option[URI]   = tryParse(uri).toOption
  }

  implicit private[files] final class RichCharSequence(val s: CharSequence) extends AnyVal {
    def isBlank: Boolean = {
      if (s.isNull) return true

      var i: Int   = 0
      val len: Int = s.length

      while (i < len) {
        if (!Character.isWhitespace(s.charAt(i))) return false
        i += 1
      }

      true
    }

    def isNotBlank: Boolean = !isBlank
    def nonBlank: Boolean   = !isBlank
  }

  implicit private[files] final class RichString(val s: String) extends AnyVal {
    def toBlankOption: Option[String] =
      if (new RichCharSequence(s).isNotBlank) Some(s)
      else None

    def requireLeading(lead: String): String = if (s.startsWith(lead)) s else lead + s
  }

  import rich._

  private[files] implicit def toRichFile(file: java.io.File): RichFile       = new RichFile(file)
  private[files] implicit def toRichPath(path: java.nio.file.Path): RichPath = new RichPath(path)
}
