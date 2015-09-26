package better.files

import scala.language.reflectiveCalls

/**
 * automatic resource management
 */
package object arm {
  type Closeable = {
    def close(): Unit
  }

  /**
   * Lightweight automatic resource management
   * Closes the resource when done
   * e.g.
   * <pre>
   * {@code
   * for {
   *   in <- managed(file.newInputStream)
   * } in.write(bytes)
   * // in is closed now
   * </code>
   * @param resource
   * @return
   */
  def managed[A <: Closeable](resource: A): Traversable[A] = new Traversable[A] {
    override def foreach[U](f: A => U) = try {
      f(resource)
    } finally {
      resource.close()
    }
  }
}
