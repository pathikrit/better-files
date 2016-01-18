package better.files
import java.util.stream.{Stream => JStream}

/*
 * Provides an iterator that auto-closes the underlying java
 * stream, to avoid leaking open file descriptors.
 * 
 * If the stream is not depleted, it will not auto-close.
 *  
 */
object AutoClosingStream {
  
  def apply(jstream: JStream[java.nio.file.Path]): Iterator[File] = {
    
    val streamIterator = jstream.iterator
  
    def get: Option[File] = {
      streamIterator.hasNext match {
        case true => Some(new File(streamIterator.next))
        case false =>
          jstream.close
          None
      }
    }
    
    Iterator.continually(get).takeWhile(_.isDefined).map(_.get)
  }
}