package com.oxygenxml.git.auth;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.Arrays;

import org.eclipse.jgit.transport.sshd.DefaultProxyDataFactory;
import org.eclipse.jgit.transport.sshd.ProxyData;

/**
 * Obtain {@link ProxyData} to connect through some proxy.
 * 
 * An attempt will be made to resolve the host name into an InetAddress. 
 */
public class ResolvingProxyDataFactory extends DefaultProxyDataFactory {

  /**
   * org.eclipse.jgit.transport.sshd.ProxyDataFactory.get(InetSocketAddress)
   */
  @Override
  public ProxyData get(InetSocketAddress remoteAddress) {
    ProxyData proxyData = super.get(remoteAddress);
    return newData(proxyData);
  }


  /**
   * Create new proxy data over the given one. Inspired by EGit ProxyDataFactory.
   * 
   * @param data Proxy data.
   * 
   * @return new proxy data or <code>null</code> if the given data is <code>null</code>.
   */
  private ProxyData newData(ProxyData data) {
    if (data == null) {
      return null;
    }

    ProxyData newProxyData = null;
    Proxy proxy = data.getProxy();
    SocketAddress addr = proxy.address();
    if (proxy.type() == Proxy.Type.DIRECT || !(addr instanceof InetSocketAddress)) {
      newProxyData = data;
    } else {
      InetSocketAddress address = (InetSocketAddress) addr;
      InetSocketAddress proxyAddress = new InetSocketAddress(address.getHostName(), address.getPort());
      char[] password = null;
      try {
        password = data.getPassword() == null ? null : data.getPassword();
        switch (proxy.type()) {
          case HTTP:
            proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
            newProxyData = new ProxyData(proxy, data.getUser(), password);
            break;
          case SOCKS:
            proxy = new Proxy(Proxy.Type.SOCKS, proxyAddress);
            newProxyData = new ProxyData(proxy, data.getUser(), password);
            break;
          default:
            break;
        }
      } finally {
        if (password != null) {
          Arrays.fill(password, '\000');
        }
      }
    }
    
    return newProxyData;
  }

}
