package com.oxygenxml.git.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;

public class RepoUtil {

  /**
   * Hidden constructor.
   */
  private RepoUtil() {
    // Avoid instantiation 
  }
  
  /**
   * Get the repository corresponding to the given file.
   * We search for only one file because in oXygen all files from the Project view
   * are in the same project/repository.
   * 
   * @param file The file.
   * 
   * @return the repository, or <code>null</code> if couldn't be detected.
   */
  public static String getRepositoryForFile(File file) {
    String repository = null;
    while (repository == null && file.getParent() != null) {
      if (FileHelper.isGitRepository(file.getPath())) {
        repository = file.getAbsolutePath();
      }
      file = file.getParentFile();
    }
    return repository;
  }
  
  /**
   * Update current repository. Set the given one as the current.
   * 
   * @param repository The repository to set as current.
   * 
   * @throws IOException
   */
  public static void updateCurrentRepository(String repository) throws IOException {
    String previousRepository = OptionsManager.getInstance().getSelectedRepository();
    if (!repository.equals(previousRepository)) {
      GitAccess.getInstance().setRepositorySynchronously(repository);
    }
  }
  
  /**
   * Get file path relative to repository.
   * 
   * @param selFile    Selected file.
   * @param repository Repository location.
   * 
   * @return the relative path. Never <code>null</code>.
   */
  public static String getFilePathRelativeToRepo(File selFile, String repository) {
    Path repo = Paths.get(repository);
    Path file = Paths.get(selFile.getAbsolutePath());
    return repo.relativize(file).toString();
  }
  
}
