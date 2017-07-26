package com.oxygenxml.git.protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Handler for the file2 protocol
 */
public class CustomProtocolHandler extends URLStreamHandler {
  /**
   * Connection class for file2
   */
  private static class File2Connection extends URLConnection {
    /**
     * Construct the connection
     * 
     * @param url
     *          The URL
     */
    protected File2Connection(URL url) {

      super(url);
      // Allow output
      setDoOutput(true);
    }
    
    /**
     * @see java.net.URLConnection#getURL()
     */
    @Override
    public URL getURL() {
      URL toReturn = super.getURL();
      String content = toReturn.toString();
      if(content.indexOf("?") != -1) {
        //Remove the parameters part
        if(content.indexOf("#") != -1) {
          //But keep the anchor in place
          content = content.substring(0, content.indexOf("?")) + content.substring(content.indexOf("#"), content.length());
        } else {
          content = content.substring(0, content.indexOf("?"));
        }
      }
      return toReturn;
    }

    /**
     * Returns an input stream that reads from this open connection.
     * 
     * @return the input stream
     */
    @Override
    public InputStream getInputStream() throws IOException {
      File file = getCanonicalFileFromFileUrl(url);
      final FileInputStream fis = new FileInputStream(file);
      return fis;
    }

    /**
     * Returns an output stream that writes to this connection.
     * 
     * @return the output stream
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
      File file = getCanonicalFileFromFileUrl(url);
      final FileOutputStream fos = new FileOutputStream(file);
      OutputStream os = new FilterOutputStream(fos) {
        /**
         * @see java.io.FilterOutputStream#write(byte[])
         */
        @Override
        public void write(byte[] b) throws IOException {
          super.write(b);
        }
        /**
         * @see java.io.FilterOutputStream#write(byte[], int, int)
         */
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
          super.write(b, off, len);
        }
        /**
         * @see java.io.FilterOutputStream#write(int)
         */
        @Override
        public void write(int b) throws IOException {
          super.write(b);
        }
      };
      return os;
    }

    /**
     * Opens a communications link to the resource referenced by this URL, if such a connection has
     * not already been established.
     */
    @Override
    public void connect() throws IOException {
      this.connected = true;
    }
    
    /**
     * @see java.net.URLConnection#getContentLength()
     */
    @Override
    public int getContentLength() {
      File file = getCanonicalFileFromFileUrl(url);
      return (int) file.length();
    }
  }

  /**
   * Creates and opens the connection
   * 
   * @param u
   *          The URL
   * @return The connection
   */
  @Override
  protected URLConnection openConnection(URL u) throws IOException {
    URLConnection connection = new File2Connection(u);
    return connection;
  }
  
  /**
   * On Windows names of files from network neighborhood must be corrected before open.
   * 
   * @param url
   *          The file URL.
   * @return The cannonical or absolute file.
   * @throws IllegalArgumentException
   *           if the URL is not file.
   */
  public static File getCanonicalFileFromFileUrl(URL url) {
    File file = null;
    if (url == null) {
      throw new NullPointerException("The URL cannot be null.");
    }
    try {
      URI uri = new URI(url.toString().replace("cproto", "file"));
      file = new File(uri);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return file;
  }
}
