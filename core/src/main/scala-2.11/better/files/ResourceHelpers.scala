package better.files

import java.net.URL

/**
  * Routines needed by [[Resource]] that are specific to the Scala version in use.
  *
  * This is the Scala 2.11 version.  It implements ResourceLoader with a classic inner class.
  */
private[files] object ResourceHelpers {
  @inline
  def from(cl: ClassLoader): ResourceLoader = new ResourceLoader {
    override def url(name: String): Option[URL] =
      Option(cl.getResource(name))
  }
}
