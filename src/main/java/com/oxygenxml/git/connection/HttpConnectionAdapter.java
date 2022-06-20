package com.oxygenxml.git.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.eclipse.jgit.transport.http.HttpConnection;

/**
 * Adapter for HttpConnection
 * 
 * @author cristi_talau
 */
public class HttpConnectionAdapter implements HttpConnection {

  /**
   * The delegate HttpConnection.
   */
  protected HttpConnection delegate;

  /**
   * Constructor. 
   * @param delegate The delegate HttpConnection.
   */
  public HttpConnectionAdapter(HttpConnection delegate) {
    this.delegate = delegate;
  }
  
  public int getResponseCode() throws IOException {
    return this.delegate.getResponseCode();
  }

  public URL getURL() {
    return this.delegate.getURL();
  }

  public String getResponseMessage() throws IOException {
    return this.delegate.getResponseMessage();
  }

  public Map<String, List<String>> getHeaderFields() {
    return this.delegate.getHeaderFields();
  }

  public void setRequestProperty(String key, String value) {
    this.delegate.setRequestProperty(key, value);
  }

  public void setRequestMethod(String method) throws ProtocolException {
    this.delegate.setRequestMethod(method);
  }

  public void setUseCaches(boolean usecaches) {
    this.delegate.setUseCaches(usecaches);
  }

  public void setConnectTimeout(int timeout) {
    this.delegate.setConnectTimeout(timeout);
  }

  public void setReadTimeout(int timeout) {
    this.delegate.setReadTimeout(timeout);
  }

  public String getContentType() {
    return this.delegate.getContentType();
  }

  public InputStream getInputStream() throws IOException {
    return this.delegate.getInputStream();
  }

  public String getHeaderField(String name) {
    return this.delegate.getHeaderField(name);
  }

  public int getContentLength() {
    return this.delegate.getContentLength();
  }

  public void setInstanceFollowRedirects(boolean followRedirects) {
    this.delegate.setInstanceFollowRedirects(followRedirects);
  }

  public void setDoOutput(boolean dooutput) {
    this.delegate.setDoOutput(dooutput);
  }

  public void setFixedLengthStreamingMode(int contentLength) {
    this.delegate.setFixedLengthStreamingMode(contentLength);
  }

  public OutputStream getOutputStream() throws IOException {
    return this.delegate.getOutputStream();
  }

  public void setChunkedStreamingMode(int chunklen) {
    this.delegate.setChunkedStreamingMode(chunklen);
  }

  public String getRequestMethod() {
    return this.delegate.getRequestMethod();
  }

  public boolean usingProxy() {
    return this.delegate.usingProxy();
  }

  public void connect() throws IOException {
    this.delegate.connect();
  }

  public void configure(KeyManager[] km, TrustManager[] tm, SecureRandom random)
      throws NoSuchAlgorithmException, KeyManagementException {
    this.delegate.configure(km, tm, random);
  }

  public void setHostnameVerifier(HostnameVerifier hostnameverifier)
      throws NoSuchAlgorithmException, KeyManagementException {
    this.delegate.setHostnameVerifier(hostnameverifier);
  }

  @Override
  public List<String> getHeaderFields(String name) {
    return this.delegate.getHeaderFields(name);
  }
}
