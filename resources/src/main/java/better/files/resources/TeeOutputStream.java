package better.files.resources;

import java.io.OutputStream;

/**
 * This is just org.apache.commons.io.output.TeeOutputStream
 * 
 * This cannot be a Scala class because the ScalaSignature will contain a reference to
 * org.apache.commons.io.output.TeeOutputStream even though Proguard rewrites the name.
 */
public class TeeOutputStream extends org.apache.commons.io.output.TeeOutputStream {
  public TeeOutputStream(OutputStream out, OutputStream branch) { super(out, branch); }
}
