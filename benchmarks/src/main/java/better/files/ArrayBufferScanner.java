package better.files;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

/**
 * Hand built using a char buffer
 */
public class ArrayBufferScanner extends AbstractScanner {
  private char[] buffer = new char[1<<4];
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
      if (c != ' ' && c != '\n' && c != '\t' && c != '\r' && c != '\f') {
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
    try {
      loadBuffer();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    // SOURCE COPIED FROM Java's Integer.parseInt
    int radix = 10;
    int result = 0;
    boolean negative = false;
    int i = 0, len = pos;
    int limit = -Integer.MAX_VALUE;
    int multmin;
    int digit;

    if (len > 0) {
      char firstChar = buffer[0];
      if (firstChar < '0') { // Possible leading "+" or "-"
        if (firstChar == '-') {
          negative = true;
          limit = Integer.MIN_VALUE;
        } else if (firstChar != '+')
          throw new NumberFormatException(String.copyValueOf(buffer, 0, pos));

        if (len == 1) // Cannot have lone "+" or "-"
          throw new NumberFormatException(String.copyValueOf(buffer, 0, pos));
        i++;
      }
      multmin = limit / radix;
      while (i < len) {
        // Accumulating negatively avoids surprises near MAX_VALUE
        digit = Character.digit(buffer[i++],radix);
        if (digit < 0) {
          throw new NumberFormatException(String.copyValueOf(buffer, 0, pos));
        }
        if (result < multmin) {
          throw new NumberFormatException(String.copyValueOf(buffer, 0, pos));
        }
        result *= radix;
        if (result < limit + digit) {
          throw new NumberFormatException(String.copyValueOf(buffer, 0, pos));
        }
        result -= digit;
      }
    } else {
      throw new NumberFormatException(String.copyValueOf(buffer, 0, pos));
    }
    return negative ? result : -result;
  }
}
