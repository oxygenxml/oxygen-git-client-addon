package com.oxygenxml.git.auth;

import java.net.InetSocketAddress;
import java.net.Proxy;
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
   * Create new proxy data over the given.
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

}
