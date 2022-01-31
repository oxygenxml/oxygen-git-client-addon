package com.oxygenxml.git.protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.Commit;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.utils.FileUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * Handler for the "git" protocol. Can be used to for the three way diff on the
 * remote commit, last local commit and the base. It is also used for the 2 way
 * diff with the Last Local Commit
 * 
 */
public class GitRevisionURLHandler extends URLStreamHandler {

	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(GitRevisionURLHandler.class);
	
	/**
	 * The git protocol
	 */
	public static final String GIT_PROTOCOL = "git";
	
	/**
	 * Connection class for XML files in archives.
	 */
	private static class GitRevisionConnection extends URLConnection {

		/**
		 * The "file object" used to get the input stream from. The jgit library
		 * uses this kind of object(ObjectId) to point to commits.
		 */
		private ObjectId fileObject;

		/**
		 * Path obtained from the URL. The path is relative to the selected
		 * repository
		 */
		private String path;

		/**
		 * The host which is used to let the user write in the diff tool
		 */
		private String currentHost;

		/**
		 * Construct the connection
		 * 
		 * @param url The URL.
		 * 
		 * @throws IOException
		 */
		protected GitRevisionConnection(URL url) throws IOException {
			super(url);
			setDoOutput(true);
				
			decode(url);
		}

		/**
		 * Extracts the Git identifiers from the URL.
		 * 
		 * @param url  URL to get the host.
		 * 
		 * @throws IOException
		 */
		private void decode(URL url) throws IOException {
		  // Decode the URL first.
		  if (PluginWorkspaceProvider.getPluginWorkspace() != null && 
		      PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess() != null) {
		    UtilAccess utilAccess = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess();
		    url = new URL(utilAccess.uncorrectURL(url.toExternalForm()));
		  }
		  
			path = url.getPath();
			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			currentHost = url.getHost();
			if(currentHost.equals("CurrentSubmodule") || currentHost.equals("PreviousSubmodule")) {
				path = path.replace(".txt", "");
			}
			
			GitAccess gitAccess = GitAccess.getInstance();
			
			switch(currentHost) {
			  case VersionIdentifier.MINE:
			  case VersionIdentifier.MINE_RESOLVED:
			    fileObject = gitAccess.getCommit(Commit.MINE, path);
			    break;
			  case VersionIdentifier.INDEX_OR_LAST_COMMIT:
			    try {
	          fileObject = gitAccess.locateObjectIdInIndex(path);
	        } catch (Exception ex) {
	          LOGGER.error(ex.getMessage(), ex);
	        }
	        if (fileObject == null) {
	          fileObject = gitAccess.getCommit(Commit.LOCAL, path);
	        }
			    break;
			  case VersionIdentifier.LAST_COMMIT:
			    fileObject = gitAccess.getCommit(Commit.LOCAL, path);
			    break;
			  case VersionIdentifier.THEIRS:
			  case VersionIdentifier.MINE_ORIGINAL:
			    fileObject = gitAccess.getCommit(Commit.THEIRS, path);
			    break;
			  case VersionIdentifier.BASE:
			    fileObject = gitAccess.getCommit(Commit.BASE, path);
			    break;
			  case VersionIdentifier.CURRENT_SUBMODULE:
			    fileObject = gitAccess.getSubmoduleAccess().submoduleCompare(path, false);
			    break;
			  case VersionIdentifier.PREVIOUSLY_SUBMODULE:
			    fileObject = gitAccess.getSubmoduleAccess().submoduleCompare(path, true);
			    break;
			  default:
	        // Probably an ID.
	        try {
	          fileObject = RevCommitUtil.getObjectID(gitAccess.getRepository(), currentHost, path);
	        } catch (IOException | NoRepositorySelected e) {
	          throw new IOException("Unable to extract GIT data from: " + getURL(), e);
	        }
			    break;
			}
			
			if (fileObject == null) {
			  throw new IOException("Unable to obtain commit ID for: " + getURL());
			}
		}

		/**
		 * Returns an input stream that reads from this open connection.
		 * 
		 * @return the input stream
		 */
		@Override
		public InputStream getInputStream() throws IOException {
			if (VersionIdentifier.CURRENT_SUBMODULE.equals(currentHost) 
					|| VersionIdentifier.PREVIOUSLY_SUBMODULE.equals(currentHost)) {
				String commit = "Subproject commit " + fileObject.getName();
				File temp = File.createTempFile("submodule", ".txt");
				PrintWriter printWriter = new PrintWriter(temp);
				printWriter.println(commit);
				printWriter.close();
				return new FileInputStream(temp);
			}
			
			return GitAccess.getInstance().getInputStream(fileObject);
		}

		/**
		 * Returns an output stream that writes to this connection.
		 * 
		 * @return the output stream
		 */
		@Override
		public OutputStream getOutputStream() throws IOException {
			if (VersionIdentifier.MINE.equals(currentHost) || VersionIdentifier.MINE_RESOLVED.equals(currentHost)) {
        try {
          URL fileContent = FileUtil.getFileURL(path);
          return fileContent.openConnection().getOutputStream();
        } catch (NoWorkTreeException | NoRepositorySelected e) {
          throw new IOException(e);
        }
			}
			throw new IOException("Writing is permitted only in the Working Copy files.");
		}

		@Override
		public void connect() throws IOException {
		  // Nothing to do.
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
	@Override
  protected URLConnection openConnection(URL u) throws IOException {
		return new GitRevisionConnection(u);
	}

	/**
	 * Constructs an URL for the diff tool
	 * 
	 * @param locationHint A constant from {@link VersionIdentifier}
	 * @param fileLocation The file location relative to the repository
	 * @return the URL of the form git://gitFile/fileLocation
	 * 
	 * @throws MalformedURLException Unnable to build the URL.
	 */
	public static URL encodeURL(String locationHint, String fileLocation) throws MalformedURLException {
		URL url = new URL ("git://" + locationHint + "/" + fileLocation);
		if(locationHint.equals(VersionIdentifier.CURRENT_SUBMODULE) || locationHint.equals(VersionIdentifier.PREVIOUSLY_SUBMODULE)) {
			// Add an extension to mimic a file. The content of this file will be the commit ID linked with the SUBMODULE.
			url = new URL("git://" + locationHint + "/" + fileLocation +".txt");
		}

		return url;
	}
}
