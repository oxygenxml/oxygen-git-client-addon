package com.oxygenxml.git.service.entities;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.protocol.GitRevisionURLHandler;
import com.oxygenxml.git.protocol.VersionIdentifier;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.utils.FileUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * Class with usefully methods for files statues.
 * 
 * @author Alex_Smarandache
 *
 */
public class FileStatusUtil {

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(FileStatusUtil.class);


  /**
   * Hidden constructor.
   */
  private FileStatusUtil() {
    // nothing.
  }

  /**
   * Map between the {@link ChangeType} types and our {@link GitChangeType}
   * 
   * @param diffChange Comparison data.
   * 
   * @return The type of change.
   */
  public static GitChangeType toGitChangeType(ChangeType diffChange) {
    GitChangeType toReturn = GitChangeType.ADD;
    if (ChangeType.DELETE == diffChange) {
      toReturn = GitChangeType.REMOVED;
    } else if (ChangeType.MODIFY == diffChange) {
      toReturn = GitChangeType.CHANGED;
    } else if (isRename(diffChange)) {
      toReturn = GitChangeType.RENAME;
    }

    return toReturn;
  }


  /**
   * Computes a list of files statues URLs.
   * 
   * @param files          The files.
   * @param computeGitURLs <code>true</code> if the computed URL should be a git URL, <code>false</code> if the URL should be from the WC.
   * 
   * @return The computed URLs.
   */
  public static List<URL> getFilesStatuesURL(@NonNull final List<FileStatus> files, 
      final boolean computeGitURLs) {
    final List<URL> filesURL = new ArrayList<>();
    files.forEach(file -> {
      try {
        filesURL.add(computeGitURLs ? computeFileStatusURL(file) :
          FileUtil.getFileURL(file.getFileLocation()));
      } catch (NoRepositorySelected | MalformedURLException e) {
        LOGGER.debug(e.getMessage(), e);
      }
    });

    return filesURL;
  }

  /**
   * Computes URL for the given file.
   * 
   * @param file File to compute URL.
   * 
   * @return Computed URL.
   * 
   * @throws MalformedURLException
   * @throws NoRepositorySelected
   */
  public static URL computeFileStatusURL(final FileStatus file) throws MalformedURLException, NoRepositorySelected {
    return shouldComputeGitURL(file) ? GitRevisionURLHandler.encodeURL(VersionIdentifier.INDEX_OR_LAST_COMMIT,
        file.getFileLocation()) : FileUtil.getFileURL(file.getFileLocation());
  }

  /**
   * Checks if is needed to compute or not a Git URL for the given files. 
   * If the file is from index and a new change is present to the unstaged files, a Git URL should be computed.
   * 
   * @param file The file status.
   * 
   * @return <code>true</code> if a Git URL should be computed.
   */
  private static boolean shouldComputeGitURL(final FileStatus file) {
    return (file.getChangeType() == GitChangeType.ADD || file.getChangeType() == GitChangeType.CHANGED) 
        && GitAccess.getInstance().getUnstagedFiles().stream().anyMatch(fileStatus-> 
          fileStatus.getFileLocation() != null && fileStatus.getFileLocation().equals(file.getFileLocation()))
        && GitAccess.getInstance().getStagedFiles().stream().anyMatch(fileStatus-> 
          fileStatus.getFileLocation() != null && fileStatus.getFileLocation().equals(file.getFileLocation()));
  }

  /**
   * Checks the change type to see if it represents a rename.
   * 
   * @param diffChange The ChangeType.
   * 
   * @return <code>true</code> if this change represents a rename.
   */
  public static boolean isRename(ChangeType diffChange) {
    return diffChange == ChangeType.RENAME
        || diffChange == ChangeType.COPY;
  }

  /**
   * Remove all files statues with the given extension.
   * 
   * @param files     The files.
   * @param extension The extension.
   * 
   * @return The files without files with the given extension.
   */
  @NonNull 
  public static List<FileStatus> removeFilesByExtension(@NonNull final List<FileStatus> files,
      final String extension) {
    if(extension != null && !extension.isEmpty()) {
      files.removeIf(file -> file.getFileLocation() != null && file.getFileLocation().endsWith(extension));
    }
    return files;
  }

  /**
   * Check if a file is unreachable.
   * <br>
   * A file is considered to be unreachable if is a binary resource or has an unmapped type. 
   * 
   * @param file The file to be checked.
   * 
   * @return <code>true</code> if the file is unreachable.
   */
  public static boolean isUnreachableFile(@NonNull FileStatus file) {
    final UtilAccess utilAccess = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess();
    try {
      final URL fileURL = FileUtil.getFileURL(file.getFileLocation());
      return utilAccess.isUnhandledBinaryResourceURL(fileURL) 
          || Objects.isNull(utilAccess.getContentType(fileURL.toExternalForm()));
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      return false;
    }

  }
}
