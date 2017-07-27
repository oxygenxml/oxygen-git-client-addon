package com.oxygenxml.git.protocol;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.jgit.lib.ObjectId;

import com.oxygenxml.git.service.GitAccess;

import ro.sync.exml.editor.ContentTypes;

/**
 * Handler for the "filexmlzip" protocol. Can be used to open/edit XML files
 * directly from ZIPs
 */
public class GitRevisionURLHandler extends URLStreamHandler {

	/**
	 * The filexmlzip protocol
	 */
	public static final String GIT_PROTOCOL = "git";

	/**
	 * Connection class for XML files in archives.
	 */
	private static class GitRevisionConnection extends URLConnection {

		private static final String LOCAL = "Local";
		private static final String REMOTE = "Remote";
		private static final String BASE = "Base";
		private static final String LAST_COMMIT = "LastCommit";

		private ObjectId revision;
		private String path;
		private String hostInitiator;

		/**
		 * Construct the connection
		 * 
		 * @param url
		 *          The URL
		 */
		protected GitRevisionConnection(URL url) throws IOException {
			super(url);
			// TODO Allow output only for LOCAL.
			setDoOutput(true);
			GitAccess gitAccess = GitAccess.getInstance();

			try {


				path = url.getPath();

				if (path.startsWith("/")) {
					path = path.substring(1);
				}

				String host = url.getHost();

				if (LOCAL.equals(host)) {
					revision = gitAccess.getLastLocalCommit();
					hostInitiator = LOCAL;
				} else if (LAST_COMMIT.equals(host)) {
					revision = gitAccess.getLastLocalCommit();
					hostInitiator = LAST_COMMIT;
				} else if (REMOTE.equals(host)) {
					revision = gitAccess.getRemoteCommit();
					hostInitiator = REMOTE;
				}else if (BASE.equals(host)) {
					revision = gitAccess.getBaseCommit();
					hostInitiator = BASE;
				} else {
					throw new Exception("Bad syntax: " + path);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		/**
		 * Returns an input stream that reads from this open connection.
		 * 
		 * @return the input stream
		 */
		public InputStream getInputStream() throws IOException {
			return GitAccess.getInstance().getLoaderFrom(revision, path).openStream();
		}

		/**
		 * Returns an output stream that writes to this connection.
		 * 
		 * @return the output stream
		 */
		public OutputStream getOutputStream() throws IOException {
			if (LOCAL.equals(hostInitiator)) {
				URL fileContent = GitAccess.getInstance().getFileContent(path);
				return fileContent.openConnection().getOutputStream();
			}
			return null;
		}

		public void connect() throws IOException {
		}

		/**
		 * @see java.net.URLConnection#getContentLength()
		 */
		@Override
		public int getContentLength() {
			return -1;
		}

		/**
		 * @see java.net.URLConnection#getContentType()
		 */
		@Override
		public String getContentType() {
			// LEt Oxygen decide.
			return null;
		}
	}

	/**
	 * Creates and opens the connection
	 * 
	 * @param u
	 *          The URL
	 * @return The connection
	 */
	protected URLConnection openConnection(URL u) throws IOException {
		URLConnection connection = new GitRevisionConnection(u);
		return connection;
	}

}
