package better.files;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

/**
 * Fastest scanner I can build
 */
public class ArrayBufferScanner extends AbstractScanner {
  private char[] buffer = new char[20];
  private int pos = 0;

  private BufferedReader reader;

  public ArrayBufferScanner(BufferedReader reader) {
    super(reader);
    this.reader = reader;
  }

  @Override
  public boolean hasNext() {
    return pos != -1;
  }

  private void loadBuffer() throws IOException {
    while(true) {
      int i = reader.read();
      if (i == -1) {
        pos = -1;
        break;
      }
      char c = (char) i;
      if (c == ' ' || c == '\n') {
        break;
      }
      if (pos == buffer.length) {
        buffer = Arrays.copyOf(buffer, 2 * pos);
      }
      buffer[pos++] = c;
    }
  }

  @Override
  public String next() {
    boolean found = false;
    while(!found && hasNext()) {
      pos = 0;
      try {
        loadBuffer();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      found = pos > 0;
    }
    return String.copyValueOf(buffer, 0, pos);
  }

  @Override
  public String nextLine() {
    try {
      return reader.readLine();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public int nextInt() {
    return Integer.parseInt(next());
  }
}
