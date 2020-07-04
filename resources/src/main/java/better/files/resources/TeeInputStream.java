package better.files.resources;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is just org.apache.commons.io.input.TeeInputStream
 * 
 * This cannot be a Scala class because the ScalaSignature will contain a reference to
 * org.apache.commons.io.input.TeeInputStream even though Proguard rewrites the name.
 */
public class TeeInputStream extends org.apache.commons.io.input.TeeInputStream {
  public TeeInputStream(InputStream out, OutputStream branch) { super(out, branch); }
  public TeeInputStream(InputStream out, OutputStream branch, boolean closeBranch) { super(out, branch, closeBranch); }
}
