package com.oxygenxml.git.protocol;

import java.net.URL;
import java.net.URLStreamHandler;

import ro.sync.exml.plugin.lock.LockHandler;
import ro.sync.exml.plugin.urlstreamhandler.URLHandlerReadOnlyCheckerExtension;
import ro.sync.exml.plugin.urlstreamhandler.URLStreamHandlerWithLockPluginExtension;

/**
 * Plugin extension - custom protocol URL handler extension
 */
public class CustomProtocolURLHandlerExtension
		implements URLStreamHandlerWithLockPluginExtension, URLHandlerReadOnlyCheckerExtension {

	/**
	 * The git protocol name.
	 */
	private static final String GIT = "git";

	/**
	 * Gets the handler for the custom protocol
	 */
	@Override
  public URLStreamHandler getURLStreamHandler(String protocol) {
		// If the protocol is "git" return its handler
		if (protocol.equals(GIT)) {
			return new GitRevisionURLHandler();
		}
		return null;
	}

	/**
	 * @see ro.sync.exml.plugin.urlstreamhandler.URLStreamHandlerWithLockPluginExtension#getLockHandler()
	 */
	@Override
  public LockHandler getLockHandler() {
		return null;
	}

	/**
	 * @see ro.sync.exml.plugin.urlstreamhandler.URLStreamHandlerWithLockPluginExtension#isLockingSupported(java.lang.String)
	 */
	@Override
  public boolean isLockingSupported(String protocol) {
		return false;
	}

	/**
	 * @see ro.sync.exml.plugin.urlstreamhandler.URLHandlerReadOnlyCheckerExtension#canCheckReadOnly(java.lang.String)
	 */
	@Override
  public boolean canCheckReadOnly(String protocol) {
		return GIT.equals(protocol);
	}

	/**
	 * @see ro.sync.exml.plugin.urlstreamhandler.URLHandlerReadOnlyCheckerExtension#isReadOnly(java.net.URL)
	 */
	@Override
	public boolean isReadOnly(URL url) {
	  boolean isReadOnly = false;
    if (GIT.equals(url.getProtocol()) && !VersionIdentifier.MINE.equals(url.getHost())
        && !VersionIdentifier.MINE_RESOLVED.equals(url.getHost())) {
      isReadOnly = true;
    }

	  return isReadOnly;
	}
}