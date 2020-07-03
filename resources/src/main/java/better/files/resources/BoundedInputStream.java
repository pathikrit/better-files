package better.files.resources;

import java.io.InputStream;

/**
 * This is just org.apache.commons.io.input.BoundedInputStream
 * 
 * This cannot be a Scala class because the ScalaSignature will contain a reference to
 * org.apache.commons.io.input.BoundedInputStream even though Proguard rewrites the name.
 */
public class BoundedInputStream extends org.apache.commons.io.input.BoundedInputStream {
  public BoundedInputStream(InputStream is) { super(is); }
  public BoundedInputStream(InputStream is, long size) { super(is, size); }
}
