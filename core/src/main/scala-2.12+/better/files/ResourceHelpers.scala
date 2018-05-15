package better.files

/**
  * Routines needed by [[Resource]] that are specific to the Scala version in use.
  *
  * This is the Scala 2.12+ version. It implements ResourceLoader with a lambda, instead of an inner class.
  */
private[files] object ResourceHelpers {
  @inline
  def from(cl: ClassLoader): ResourceLoader =
    name => Option(cl.getResource(name))
}
