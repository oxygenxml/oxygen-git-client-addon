package com.oxygenxml.git.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.xml.sax.SAXException;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.sax.XPRHandler;
import com.oxygenxml.git.service.GitAccess;

/**
 * Utility methods for detecting a Git repository and repository related issues.
 * @author alex_jitianu
 */
public class RepoUtil {
  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(RepoUtil.class);

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
      if (logger.isDebugEnabled()) {
        logger.debug(e1, e1);
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
        && !FileHelper.isGitRepository(candidate)) {
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
        if (FileHelper.isURL(path)) {
          try {
            file = new File(new URL(path).toURI());
          } catch (MalformedURLException | URISyntaxException e) {
            logger.error(e, e);
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
      if (FileHelper.isGitRepository(file)) {
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
}
