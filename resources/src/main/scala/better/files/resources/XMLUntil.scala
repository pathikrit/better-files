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

import com.ctc.wstx.stax.WstxInputFactory
import java.io.{File, InputStream}
import java.nio.charset.Charset
import javax.xml.stream.{XMLInputFactory, XMLStreamReader}
import javax.xml.stream.XMLStreamConstants.START_ELEMENT

object XMLUtil {
  def isXML(f: File): Boolean = InputStreamResource.forFile(f).buffered().use { isXML(_) }

  def isXML(is: InputStream): Boolean = isXML(is, true)

  def isXML(is: InputStream, useMarkReset: Boolean): Boolean = {
    val markLimit: Int = 1024

    if (useMarkReset) {
      require(is.markSupported, "Need an InputStream that supports mark()/reset()")
      is.mark(markLimit)
    }

    try {
      val wrappedIs: InputStream = if (useMarkReset) new BoundedInputStream(is, markLimit) else is

      withXMLStreamReader2(wrappedIs) { xmlStreamReader: XMLStreamReader =>
        // Check if there are any START_ELEMENT events
        while (xmlStreamReader.getEventType != START_ELEMENT) xmlStreamReader.next()

        // If we found a START_ELEMENT then this looks like XML
        xmlStreamReader.getEventType == START_ELEMENT
      }
    } catch {
      case ex: Exception => false
    } finally {
      if (useMarkReset) is.reset()
    }
  }

  def detectXMLCharset(is: InputStream): Option[Charset] = detectXMLCharset(is, true)
  def detectXMLCharset(is: InputStream, useMarkReset: Boolean): Option[Charset] =
    detectXMLCharsetName(is, useMarkReset).map { CharsetUtil.forName }

  def detectXMLCharsetName(is: InputStream): Option[String] = detectXMLCharsetName(is, true)

  /**
    * If this looks like an XML document attempt to detect it's encoding
    */
  def detectXMLCharsetName(is: InputStream, useMarkReset: Boolean): Option[String] = {
    val markLimit: Int = 1024

    if (useMarkReset) {
      require(is.markSupported, "Need an InputStream that supports mark()/reset()")
      is.mark(markLimit)
    }

    try {
      val wrappedIs: InputStream = if (useMarkReset) new BoundedInputStream(is, markLimit) else is

      withXMLStreamReader2(wrappedIs) { xmlStreamReader: XMLStreamReader =>
        // Check if there are any START_ELEMENT events
        while (xmlStreamReader.getEventType != START_ELEMENT) xmlStreamReader.next()

        // If we found a START_ELEMENT then this looks like XML
        if (xmlStreamReader.getEventType == START_ELEMENT) Option(xmlStreamReader.getEncoding())
        else None
      }
    } catch {
      case ex: Exception => None
    } finally {
      if (useMarkReset) is.reset()
    }
  }

  // Note: This is duplicated in the fm-xml project
  private def withXMLStreamReader2[T](is: InputStream)(f: XMLStreamReader => T): T = {
    val inputFactory = new WstxInputFactory()
    inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false)
    inputFactory.configureForSpeed()

    import Resource._
    Resource.using(inputFactory.createXMLStreamReader(is))(f)
  }

  def rootTag(f: File): String = InputStreamResource.forFile(f).use { rootTag }

  private def rootTag(is: InputStream): String =
    withXMLStreamReader2(is) { xmlStreamReader: XMLStreamReader =>
      // Skip to the root tag (which is the first START_ELEMENT)
      while (xmlStreamReader.getEventType != START_ELEMENT) xmlStreamReader.next()
      xmlStreamReader.getLocalName
    }
}
