package com.oxygenxml.git.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.xml.sax.SAXException;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.sax.XPRHandler;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

/**
 * Utility methods for detecting a Git repository and repository related issues.
 * @author alex_jitianu
 */
public class RepoUtil {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(RepoUtil.class);

  /**
   * Hidden constructor.
   */
  private RepoUtil() {
    // Avoid instantiation 
  }
  
  /**
   * Check if repo is merging or rebasing.
   * 
   * @param repoState The repo state.
   * 
   * @return <code>true</code> if the repository merging or rebasing.
   */
 public static boolean isRepoMergingOrRebasing(RepositoryState repoState) {
   boolean toReturn = false;
   if (repoState != null) {
     toReturn = repoState == RepositoryState.MERGING
         || repoState == RepositoryState.MERGING_RESOLVED
         || repoState == RepositoryState.REBASING
         || repoState == RepositoryState.REBASING_MERGE
         || repoState == RepositoryState.REBASING_REBASING;
   }
   return toReturn;
 }
  
  /**
   * Check if repo is in an unfinished conflict state (merging, rebasing, reverting, etc.).
   * 
   * @param repoState The repo state.
   * 
   * @return <code>true</code> if the repository is in an unfinished conflict state.
   */
 public static boolean isUnfinishedConflictState(RepositoryState repoState) {
   boolean toReturn = false;
   if (repoState != null) {
     toReturn = repoState == RepositoryState.MERGING
         || repoState == RepositoryState.MERGING_RESOLVED
         || repoState == RepositoryState.REBASING
         || repoState == RepositoryState.REBASING_MERGE
         || repoState == RepositoryState.REBASING_REBASING
         || repoState == RepositoryState.REVERTING;
   }
   return toReturn;
 }
 
 /**
  * Verify if the repo is non-conflictual and has uncommitted changes.
  * 
  * @param repoState The repo state.
  * 
  * @return <code>true</code> if the repository is not merging, not rebasing and with uncommitted changes. 
  */
 public static boolean isNonConflictualRepoWithUncommittedChanges(RepositoryState repoState) {
   GitStatus status = GitAccess.getInstance().getStatus();
   boolean repoHasUncommittedChanges = !status.getUnstagedFiles().isEmpty() 
       || !status.getStagedFiles().isEmpty();
   return repoHasUncommittedChanges && !RepoUtil.isUnfinishedConflictState(repoState);
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
      if (FileUtil.isGitRepository(file.getPath())) {
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
  
  /**
   * Checks the project directory for Git repositories.
   * 
   * @param projectFile The *.xpr file.
   * 
   * @return the repository or <code>null</code>.
   * 
   * @throws FileNotFoundException The project file doesn't exist.
   * @throws IOException A Git repository was detected but not loaded.
   */
  public static File detectRepositoryInProject(File projectFile) {
    File repoDir = null;
    // We will go up in the hierarchy, so we need the absolute path.
    projectFile = projectFile.getAbsoluteFile();
    File projectDir = projectFile.getParentFile();
    try {
      // Parse the XML file to detected the referred resources.
      SAXParserFactory saxParserFactory = new SAXParserFactoryImpl();
      saxParserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING , true);
      
      // XXE vulnerabilities fix.
      saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      saxParserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      
      SAXParser saxParser = saxParserFactory.newSAXParser();
      XPRHandler handler = new XPRHandler();
      
      saxParser.parse(projectFile, handler);
      
      repoDir = detectRepoDownwards(projectDir, handler.getPaths());
    } catch (ParserConfigurationException | SAXException | IOException e1) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(e1, e1);
      }
    }

    if (repoDir == null) {
      repoDir = detectRepoUpwards(projectDir);
    }
    
    return repoDir;
  }

  /**
   * Looks upwards in the ancestors of a directory in search for a Git repository.
   * 
   * @param directory Starting point.
   * 
   * @return The Git Working Copy directory, the one that contains a .git folder.
   * <code>null</code> if not found.
   */
  private static File detectRepoUpwards(File directory) {
    // The oxygen project might be inside a Git repository.
    // Look into the ancestors for a Git repository.
    File candidate = directory;
    while (candidate != null 
        && !FileUtil.isGitRepository(candidate)) {
      candidate = candidate.getParentFile();
    }
    
    return candidate;
  }

  /**
   * Looks downwards in a project structure and recursively searches for a git repository.
   * 
   * @param projectDir Project directory.
   * @param referedProjectPaths Paths refered in the project. Either absolute or relative paths.
   * 
   * @return The Git Working Copy directory, the one that contains a .git folder.
   * <code>null</code> if not found.
   */
  private static File detectRepoDownwards(File projectDir,  List<String> referedProjectPaths) {
    File repoDir = null;
    if (projectDir != null) {
      for (String path : referedProjectPaths) {
        File file = null;
        if (FileUtil.isURL(path)) {
          try {
            file = new File(new URL(path).toURI());
          } catch (MalformedURLException | URISyntaxException e) {
            LOGGER.error(e, e);
          }
        } else  if (".".equals(path)) {
          file = projectDir;
        } else {
          file = new File(projectDir, path);
        }

        repoDir = detectRepositoryDownwards(file);

        if (repoDir != null) {
          break;
        }
      }
    }

    return repoDir;
  }

  /**
   * Looks downwards in a file structure and recursively searches for a git repository.
   * 
   * @param file The directory in which to search.
   * 
   * @return The Git Working Copy directory, the one that contains a .git folder. 
   * <code>null</code> if not found.
   */
  private static File detectRepositoryDownwards(File file) {
    File repoDir = null;
    if (file != null) {
      if (FileUtil.isGitRepository(file)) {
        repoDir = file;
      } else if (file.isDirectory()) {
        File[] listFiles = file.listFiles();
        for (int i = 0; i < listFiles.length; i++) {
          repoDir = detectRepositoryDownwards(listFiles[i]);
          if (repoDir != null) {
            break;
          }
        }
      }
    }
    
    return repoDir;
  }

  /**
   * Recursively updates submodules.
   * 
   * @param git Current git repository.
   * 
   * @throws GitAPIException Git command falied.
   * @throws IOException Problems while iterating the modules.
   */
  public static void updateSubmodules(Git git) throws GitAPIException, IOException {
    // Update current repo.
    git.submoduleInit().call();
    git.submoduleUpdate().call();
    
    // Go recursively.
    SubmoduleWalk walk = SubmoduleWalk.forIndex(git.getRepository());
    while (walk.next()) {
      try (Repository subRepo = walk.getRepository()) {
        if (subRepo != null) {
          updateSubmodules(Git.wrap(subRepo));
        }
      }
    }
  }
  
  /**
   * Extracts a description about the submodule currently tracked commit and the previously tracked commit.
   * 
   * @param main The main repository.
   * @param submoduleStatus Submodule status.
   * 
   * @return A description about the submodule currently tracked commit and the previously tracked commit.
   */
  public static String extractSubmoduleChangeDescription(Repository main, SubmoduleStatus submoduleStatus) {
    StringBuilder b = new StringBuilder();
    String url = main.getConfig().getString(ConfigConstants.CONFIG_SUBMODULE_SECTION, submoduleStatus.getPath(), "url");
    b.append(Translator.getInstance().getTranslation(Tags.SUBMODULE)).append(": ").append(url).append("\n");
    b.append("\n");
    try (Repository submoduleRepository = SubmoduleWalk.getSubmoduleRepository(main, submoduleStatus.getPath())) {
      b.append(Translator.getInstance().getTranslation(Tags.SUBMODULE_NEW_TRACKED_COMMIT)).append("\n");
      b.append(Translator.getInstance().getTranslation(Tags.COMMIT))
        .append(": ")
        .append(submoduleStatus.getHeadId().abbreviate(GitAccess.SHORT_COMMIT_ID_LENGTH).name())
        .append("\n");
      appendCommitDetails(b, submoduleRepository.parseCommit(submoduleStatus.getHeadId()));
      
      b.append("\n");
      b.append(Translator.getInstance().getTranslation(Tags.SUBMODULE_PREVIOUS_TRACKED_COMMIT)).append("\n");
      b.append(Translator.getInstance().getTranslation(Tags.COMMIT))
        .append(": ")
        .append(submoduleStatus.getIndexId().abbreviate(GitAccess.SHORT_COMMIT_ID_LENGTH).name())
        .append("\n");
      appendCommitDetails(b, submoduleRepository.parseCommit(submoduleStatus.getIndexId()));
    } catch (IOException e) {
      LOGGER.error(e, e);
    }
    
    return b.toString();
  }

  /**
   * Appends commit details: author, message and date.
   * 
   * @param b Buffer where to append commit details.
   * @param parseCommit Commit object.
   */
  private static void appendCommitDetails(StringBuilder b, RevCommit parseCommit) {
    b.append(Translator.getInstance().getTranslation(Tags.AUTHOR)).append(": " + parseCommit.getAuthorIdent().getName()).append("\n");
    b.append(Translator.getInstance().getTranslation(Tags.COMMIT_MESSAGE_LABEL)).append(": " + parseCommit.getFullMessage()).append("\n");
    b.append(Translator.getInstance().getTranslation(Tags.DATE)).append(": ")
    .append(new SimpleDateFormat("d MMM yyyy HH:mm").format(parseCommit.getAuthorIdent().getWhen())).append("\n");
  }

}
