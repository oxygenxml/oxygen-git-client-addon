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
	public URLStreamHandler getURLStreamHandler(String protocol) {
		// If the protocol is cproto return its handler

		if (protocol.equals(GIT)) {
			URLStreamHandler handler = new GitRevisionURLHandler();
			return handler;
		}
		return null;
	}

	/**
	 * @see ro.sync.exml.plugin.urlstreamhandler.URLStreamHandlerWithLockPluginExtension#getLockHandler()
	 */
	public LockHandler getLockHandler() {
		return null;
	}

	/**
	 * @see ro.sync.exml.plugin.urlstreamhandler.URLStreamHandlerWithLockPluginExtension#isLockingSupported(java.lang.String)
	 */
	public boolean isLockingSupported(String protocol) {
		return false;
	}

	/**
	 * @see ro.sync.exml.plugin.urlstreamhandler.URLHandlerReadOnlyCheckerExtension#canCheckReadOnly(java.lang.String)
	 */
	public boolean canCheckReadOnly(String protocol) {
		return false;
	}

	/**
	 * @see ro.sync.exml.plugin.urlstreamhandler.URLHandlerReadOnlyCheckerExtension#isReadOnly(java.net.URL)
	 */
	public boolean isReadOnly(URL url) {
	  // TODO Some of our resources are indeed read only. We should implement this.
	  // com.oxygenxml.git.protocol.GitRevisionURLHandler.GitRevisionConnection.getOutputStream()
		return !CustomProtocolHandler.getCanonicalFileFromFileUrl(url).canWrite();
	}
}