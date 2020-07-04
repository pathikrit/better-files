package better.files.resources;

/**
 * This is just org.apache.commons.io.output.ByteArrayOutputStream
 * 
 * This cannot be a Scala class because the ScalaSignature will contain a reference to
 * org.apache.commons.io.output.ByteArrayOutputStream even though Proguard rewrites the name.
 */
public class ByteArrayOutputStream extends org.apache.commons.io.output.ByteArrayOutputStream {
  public ByteArrayOutputStream() { super(); }
  public ByteArrayOutputStream(int size) { super(size); }
}
