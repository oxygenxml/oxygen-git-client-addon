package com.oxygenxml.git.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.eclipse.jgit.lib.ObjectId;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.utils.FileHelper;

/**
 * Handler for the "git" protocol. Can be used to for the three way diff on the
 * remote commit, last local commit and the base
 * 
 */
public class GitRevisionURLHandler extends URLStreamHandler {

	/**
	 * The git protocol
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
				} else if (BASE.equals(host)) {
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
				URL fileContent = FileHelper.getFileURL(path);
				return fileContent.openConnection().getOutputStream();
			}
			throw new IOException("Writing is permitted only in the local file.");
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
			// Let Oxygen decide.
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
