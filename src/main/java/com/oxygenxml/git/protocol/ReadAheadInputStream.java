package com.oxygenxml.git.protocol;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A filter input stream that always reads ahead at least one byte of the underlying
 * input stream.
 * 
 * This solves a bug where JGit does not close an input stream. We use this class to
 * read past the EOF and HttpClient has a mechanism in which it releases the connection
 * once the EOF is read.
 *  
 * @author cristi_talau
 */
public class ReadAheadInputStream extends BufferedInputStream {
  /**
   * Constructor.
   * 
   * @param in The underlying input stream.
   */
  protected ReadAheadInputStream(final InputStream in) {
    super(in);
  }

  @Override
  public synchronized int read(final byte[] b, final int off, final int len) throws IOException {
    int ret = super.read(b, off, len);
    readAhead();
    return ret;
  }

  /**
   * Reads one char ahead to trigger the EOF detection mechanism of the underlying stream.
   */
  private void readAhead() {
    super.mark(1);
    try {
      super.read();
    } catch (IOException e) {
      // Ignore errors during lookahead.
    } finally {
      try {
        super.reset();
      } catch (IOException e) {
        // Ignore errors during reset.
      }
    }
  }
  
  @Override
  public synchronized int read() throws IOException {
    int ret = super.read();
    readAhead();
    return ret;
  }
  
  @Override
  public int read(byte[] b) throws IOException {
    int ret = super.read(b);
    readAhead();
    return ret;
  }
  
}
