package com.oxygenxml.git.connection;

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;

import org.eclipse.jgit.transport.HttpTransport;
import org.eclipse.jgit.transport.http.HttpConnection;
import org.eclipse.jgit.transport.http.HttpConnectionFactory;

import ro.sync.net.protocol.http.HttpExceptionWithDetails;

/**
 * Utility methods for connecting to git repositories.
 * 
 * @author alex_jitianu
 */
public class ConnectionUtil {
  /**
   * Private constructor.
   */
  private ConnectionUtil() {}

   /**
     * Installs a custom HttpConnectionFactory to workaround a JGit bug.
     * This solves a bug where JGit does not close an input stream. We use this class to
     * read past the EOF and HttpClient has a mechanism in which it releases the connection
     * once the EOF is read.
     */
    public static void installHttpConnectionFactory() {
      final HttpConnectionFactory oldFactory = HttpTransport.getConnectionFactory();
      HttpTransport.setConnectionFactory(new HttpConnectionFactory() {
        
        public HttpConnection create(URL url, Proxy proxy) throws IOException {
          return wrapConnection(oldFactory.create(url, proxy));
        }
  
        public HttpConnection create(URL url) throws IOException {
          return wrapConnection(oldFactory.create(url));
        }
  
        private HttpConnection wrapConnection(HttpConnection connection) {
          return new HttpConnectionAdapter(connection) {
            @Override
            public InputStream getInputStream() throws IOException {
              return new ReadAheadInputStream(super.getInputStream());
            }
            
            @Override
            public String getResponseMessage() throws IOException {
              try {
                return super.getResponseMessage();
              } catch (HttpExceptionWithDetails e) {
                // For compatibility with older Oxygen versions.
                return e.getReason();
              }
            }
          };
        }
      });
    }

}
