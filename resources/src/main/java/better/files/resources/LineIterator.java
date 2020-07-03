package better.files.resources;

import java.io.Reader;

/**
 * This is just org.apache.commons.io.LineIterator
 * 
 * This cannot be a Scala class because the ScalaSignature will contain a reference to
 * org.apache.commons.io.LineIterator even though Proguard rewrites the name.
 */
public class LineIterator extends org.apache.commons.io.LineIterator {
  public LineIterator(Reader reader) { super(reader); }
  
  public static void closeQuietly(LineIterator iterator) { org.apache.commons.io.LineIterator.closeQuietly(iterator); }
}
