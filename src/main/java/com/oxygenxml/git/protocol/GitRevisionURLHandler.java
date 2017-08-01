package com.oxygenxml.git.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
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

			try {
				GitAccess gitAccess = GitAccess.getInstance();
				String host = getHost(url);
				if (GitFile.LOCAL.equals(host)) {
					revision = gitAccess.getLastLocalCommit();
					hostInitiator = GitFile.LOCAL;
				} else if (GitFile.LAST_COMMIT.equals(host)) {
					revision = gitAccess.getLastLocalCommit();
					hostInitiator = GitFile.LAST_COMMIT;
				} else if (GitFile.REMOTE.equals(host)) {
					revision = gitAccess.getRemoteCommit();
					hostInitiator = GitFile.REMOTE;
				} else if (GitFile.BASE.equals(host)) {
					revision = gitAccess.getBaseCommit();
					hostInitiator = GitFile.BASE;
				} else {
					throw new Exception("Bad syntax: " + path);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		private String getHost(URL url) {
			path = url.getPath();
			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			String host = url.getHost();

			return host;
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
			if (GitFile.LOCAL.equals(hostInitiator)) {
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

	public static URL buildURL(String gitFile, String fileLocation) throws MalformedURLException {
		URL url = null;
		if (gitFile.equals(GitFile.LOCAL)) {
			url = new URL("git://Local/" + fileLocation);
		} else if (gitFile.equals(GitFile.REMOTE)) {
			url = new URL("git://Remote/" + fileLocation);
		} else if (gitFile.equals(GitFile.BASE)) {
			url = new URL("git://Base/" + fileLocation);
		} else if (gitFile.equals(GitFile.LAST_COMMIT)) {
			url = new URL("git://LastCommit/" + fileLocation);
		} else {
			url = new URL("");
		}

		return url;
	}

}
