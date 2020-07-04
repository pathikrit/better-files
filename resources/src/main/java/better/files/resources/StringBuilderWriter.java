package better.files.resources;

/**
 * This is just org.apache.commons.io.output.StringBuilderWriter
 * 
 * This cannot be a Scala class because the ScalaSignature will contain a reference to
 * org.apache.commons.io.output.StringBuilderWriter even though Proguard rewrites the name.
 */
public class StringBuilderWriter extends org.apache.commons.io.output.StringBuilderWriter {
  public StringBuilderWriter() { super(); }
  public StringBuilderWriter(int capacity) { super(capacity); }
  public StringBuilderWriter(StringBuilder sb) { super(sb); }
}
