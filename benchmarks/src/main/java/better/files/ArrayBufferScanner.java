package better.files;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

/**
 * Hand built using a char buffer
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
    pos = 0;
    while(true) {
      int i = reader.read();
      if (i == -1) {
        pos = -1;
        break;
      }
      char c = (char) i;
      if (c != ' ' && c != '\n') {
        if (pos == buffer.length) {
          buffer = Arrays.copyOf(buffer, 2 * pos);
        }
        buffer[pos++] = c;
      } else if (pos != 0) {
        break;
      }
    }
  }

  @Override
  public String next() {
    try {
      loadBuffer();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
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
