package com.oxygenxml.git.auth;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jgit.transport.sshd.ProxyData;
import org.eclipse.jgit.transport.sshd.ProxyDataFactory;

/**
 * Obtain {@link ProxyData} to connect through some proxy.
 * 
 * An attempt will be made to resolve the host name into an InetAddress. 
 */
public class ResolvingProxyDataFactory implements ProxyDataFactory {
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(ResolvingProxyDataFactory.class);
  
  /**
   * The 'socket' URI scheme.
   */
  private static final String SOCKET_URI_SCHEME = "socket";

  /**
   * org.eclipse.jgit.transport.sshd.ProxyDataFactory.get(InetSocketAddress)
   */
  @Override
  public ProxyData get(InetSocketAddress remoteAddress) {
    ProxyData proxyData = getInternal(remoteAddress);
    return newData(proxyData);
  }


  /**
   * Create new proxy data over the given. Inspired by EGit ProxyDataFactory.
   * 
   * @param data PRoxy data.
   * 
   * @return new proxy data or <code>null</code> if the given data is <code>null</code>.
   */
  private ProxyData newData(ProxyData data) {
    if (data == null) {
      return null;
    }

    Proxy proxy = data.getProxy();
    if (proxy.type() == Proxy.Type.DIRECT || !(proxy.address() instanceof InetSocketAddress)) {
      return data;
    }

    InetSocketAddress address = (InetSocketAddress) proxy.address();

    char[] password = null;
    InetSocketAddress proxyAddress = new InetSocketAddress(address.getHostName(), address.getPort());
    try {
      password = data.getPassword() == null ? null : data.getPassword();
      switch (proxy.type()) {
        case HTTP:
          proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
          return new ProxyData(proxy, data.getUser(), password);
        case SOCKS:
          proxy = new Proxy(Proxy.Type.SOCKS, proxyAddress);
          return new ProxyData(proxy, data.getUser(), password);
        default:
          return null;
      }
    } finally {
      if (password != null) {
        Arrays.fill(password, '\000');
      }
    }
  }
  
  /**
   * OXYGEN PATCH
   * 
   * Just a copy of DefaultProxyDataFactory.get() that creates proper "socket://" URIs
   * instead of wrong "socks://" URIs.
   * 
   * @param remoteAddress Remote address.
   * 
   * @return An object containing proxy data or <code>null</code>.
   */
  ProxyData getInternal(InetSocketAddress remoteAddress) {
    try {
      
      if (logger.isDebugEnabled()) {
        logger.debug("Use proxy selector " + ProxySelector.getDefault());
      }
      
      // ------------------------
      // OXYGEN PATCH START
      // Proxy.Type.SOCKS.name() was replaced with SOCKET_URI_SCHEME
      //-------------------------
      List<Proxy> proxies = ProxySelector.getDefault()
          .select(new URI(SOCKET_URI_SCHEME,
              "//" + remoteAddress.getHostString(), null)); //$NON-NLS-1$
      // ------------------------
      // OXYGEN PATCH END
      //-------------------------
      
      if (logger.isDebugEnabled()) {
        logger.debug("Got SOCKS proxies " + proxies);
      }
      
      ProxyData data = getData(proxies, Proxy.Type.SOCKS);
      if (data == null) {
        proxies = ProxySelector.getDefault()
            .select(new URI(Proxy.Type.HTTP.name(),
                "//" + remoteAddress.getHostString(), //$NON-NLS-1$
                null));
        
        if (logger.isDebugEnabled()) {
          logger.debug("Got HTTP " + proxies);
        }
        
        data = getData(proxies, Proxy.Type.HTTP);
      }
      return data;
    } catch (URISyntaxException e) {
      return null;
    }
  }

  /**
   * OXYGEN PATCH 
   * 
   * This method is identical with DefaultProxyDataFactory.getData(). All we added is a null guard at the beginning.
   * 
   * 
   * @param proxies
   * @param type
   * @return
   */
  private ProxyData getData(List<Proxy> proxies, Proxy.Type type) {
    // ------------------------
    // OXYGEN PATCH START
    // Make sure we don't pass a null 
    //-------------------------
    if (proxies == null) {
      proxies = Collections.emptyList();
    }
    // ------------------------
    // OXYGEN PATCH END
    //-------------------------
    
    Proxy proxy = proxies.stream().filter(p -> type == p.type()).findFirst()
        .orElse(null);
    if (proxy == null) {
      return null;
    }
    SocketAddress address = proxy.address();
    if (!(address instanceof InetSocketAddress)) {
      return null;
    }
    switch (type) {
    case HTTP:
      return new ProxyData(proxy);
    case SOCKS:
      return new ProxyData(proxy);
    default:
      return null;
    }
  }
}
