package com.oxygenxml.git.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * An utility class for files
 * 
 * @author Beniamin Savu
 *
 */
public class FileUtil {
  
  /**
   * Archive extensions.
   */
  private static final List<String> ARCHIVE_EXTENSIONS = Arrays.asList("zip", "jar", "ear", "war",
      "odb", "odf", "odg", "odm", "odp", "ods", "odt", "otg", "oth", "otp", "ots", "ott",
      "sxw", "sdw", "stw", "sxm", "sxi", "sti", "sxd", "std", "sxc", "stc","docx", "xlsx", "pptx",
      //word
      "dotx",  "docm", "dotm",
      //excel
      "xlsm", "xlsb", "xltx", "xltm", "xlam",
      //power point
      "pptm", "potx", "potm", "thmx", "ppsx", "ppsm", "ppam",
      "epub", "idml", "kmz");
  
  /**
   * Hidden constructor.
   */
  private FileUtil() {
    // Nothing
  }

	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);
	
	/**
   * Get the common ancestor for a list of directories.
   * 
   * @param dirs  The list of directories.
   * 
   * @return The common ancestor or <code>null</code> if 
   *            the given list of directories is 
   *            <code>null</code> or empty.
   */
	public static File getCommonDir(Set<File> dirs) {
	  // Quick check
	  if (dirs == null || dirs.isEmpty()) {
	    return null;
	  }
	  
	  int k = 0;
	  String[][] folders = new String[dirs.size()][];
	  for (Iterator<File> iter = dirs.iterator(); iter.hasNext();) {
	    folders[k] = iter.next().getAbsolutePath().split(Pattern.quote(File.separator));
	    k++;
	  }

	  StringBuilder commonPathBuilder = new StringBuilder();
	  for (int j = 0; j < folders[0].length; j++) {
	    String thisFolder = folders[0][j]; 
	    boolean allMatched = true;
	    for (int i = 1; i < folders.length && allMatched; i++) {
	      String[] currentLine = folders[i];
	      if (j >= currentLine.length) {
	        // The first line contains more tokens than the current one.
	        allMatched = false;
	        break;
	      } else {
	        allMatched &= currentLine[j].equals(thisFolder);
	      }
	    }
	    if (allMatched) {
	      commonPathBuilder.append(thisFolder).append("/");
	    } else {
	      break;
	    }
	  }

	  return new File(commonPathBuilder.toString()); // NOSONAR findsecbugs:PATH_TRAVERSAL_IN
	}
	
	/**
	 * Makes sure the path uses just the / separator.
	 * 
	 * @param path File path.
	 * 
	 * @return A path tat contains only / as separator.
	 */
	public static String rewriteSeparator(String path) {
	  return path.replace("\\", "/");
	}
	
	/**
   * Extracts the last file name from the path.
   * 
   * @param               path The path of the file.
   * @param removeAnchors <code>true</code> to remove any anchors (what is after a '#' sign inclusive) 
   * @return              The name of the file.
   */
  public static String extractFileName(String path, boolean removeAnchors) {
    if (path == null) {
      return null;
    }
    String fileName = null;
    if (removeAnchors) {
      // Remove first the anchor. It can contain '/' characters.
      path = removeQueryOrAnchorFromName(path);
    }
    int index = path.lastIndexOf('/');
    if (index == -1) {
      // Maybe we have a file path with the path delimiter '\'.
      index = path.lastIndexOf('\\');
    }
    if (index != -1) {
      fileName = path.substring(index + 1);
    } else {
      // This is the case of the relative FILE (i.e. "some.html").
      fileName = path;
    }
    return fileName;
  }
  
  /**
   * Remove the query or anchor from name.
   * 
   * @param name The name to remove the query from.
   * @return The name without the query or anchor in it.
   */
  public static String removeQueryOrAnchorFromName(String name) {
    if (name != null) {
      int queryIndex = name.indexOf('?');
      if(queryIndex != -1) {
        name = name.substring(0, queryIndex);
      } else {
        //Maybe it has an anchor
        int anchorIndex = name.indexOf('#');
        if(anchorIndex != -1) {
          name = name.substring(0, anchorIndex);
        }        
      }
    }
    return name;
  }

	/**
	 * Searches a given path for all files in that path. Generates the files paths
	 * relative to the given path.
	 * 
	 * @param path The path to the folder you want to search.
	 * 
	 * @return list of paths relative to the given path
	 */
	public static List<String> getAllFilesFromPath(String path) {
		File rootFolder = new File(path);  // NOSONAR findsecbugs:PATH_TRAVERSAL_IN
		List<String> fileNames = new ArrayList<>();
		if (rootFolder.isFile()) {
			fileNames.add(rewriteSeparator(rootFolder.getAbsolutePath()));
			return fileNames;
		}
		File[] listOfFiles = rootFolder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isDirectory() && (!listOfFiles[i].getName().equals(".git"))) {
				fileNames.addAll(getAllFilesFromPath(listOfFiles[i].getAbsolutePath()));
			} else if (listOfFiles[i].isFile()) {
				fileNames.add(rewriteSeparator(listOfFiles[i].getAbsolutePath()));
			}
		}
		return fileNames;
	}

	/**
	 * Returns the URL from a given path
	 * 
	 * @param path The path to get the URL.
	 * 
	 * @return the URL from the given path
	 * 
	 * @throws NoRepositorySelected 
	 */
	public static URL getFileURL(String path) throws NoRepositorySelected {
	  URL url = null;
		String selectedRepository = GitAccess.getInstance().getWorkingCopy().getAbsolutePath();
		File file = new File(selectedRepository, path);
		try {
			url = file.toURI().toURL();
		} catch (MalformedURLException e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(e.getMessage(), e);
			}
		}
		return url;
	}
	
	/**
	 * If this file is part of the repository, it returns the path relative 
	 * to the loaded Git repository.
	 * 
	 * @param file Working copy file.
	 * 
	 * @return The path relative to the repository or <code>null</code> if the file is not 
	 * from the working copy.
	 * 
	 * @throws NoRepositorySelected No repository was loaded. 
	 */
	public static String getPath(File file) throws NoRepositorySelected {
	  String selectedRepositoryPath = GitAccess.getInstance().getWorkingCopy().getAbsolutePath();
    selectedRepositoryPath = FileUtil.rewriteSeparator(selectedRepositoryPath);

    String fileInWorkPath = FileUtil.rewriteSeparator(file.getAbsolutePath());
    if (fileInWorkPath.startsWith(selectedRepositoryPath)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Notify {}", fileInWorkPath);
      }

      return fileInWorkPath.substring(selectedRepositoryPath.length () + 1);
    }
    
    return null;
  }

	/**
	 * Check if the given path corresponds to a Git repository.
	 * 
	 * @param path The path.
	 * 
	 * @return <code>true</code> if the path corresponds to a Git repository.
	 */
	public static boolean isGitRepository(String path) {
		return isGitRepository(new File(path));
	}

	 /**
   * Check if the given path corresponds to a Git repository.
   * 
   * @param folder The folder to check.
   * 
   * @return <code>true</code> if the path corresponds to a Git repository.
   */
  public static boolean isGitRepository(File folder) {
    FileFilter filter = pathname -> pathname.isDirectory() && ".git".equals(pathname.getName());
    File[] listOfFiles = folder.listFiles(filter);

		return listOfFiles != null && listOfFiles.length > 0;
  }
	
	/**
	 * Check if the given file is part of a Git repository.
	 * 
	 * @param file The file to check.
	 * 
	 * @return <code>true</code> if the given file is part of a Git repository.
	 */
  public static boolean isFromGitRepo(File file) {
    boolean isGit = false;
    File temp = file;
    while (temp.getParent() != null && !isGit) {
      if (FileUtil.isGitRepository(temp.getPath())) {
        isGit = true;
      }
      temp = temp.getParentFile();
    }
    return isGit;
  }

	/**
	 * Checks is the given path represents a submodule. A submodule contains a file named .git that 
	 * contains the path to the git dir:
	 * <pre>
	 * gitdir: ../.git/modules/js
	 * </pre>
	 * 
	 * @param path The path to check.
	 * 
	 * @return <code>true</code> if the path represents a submodule.
	 */
	public static boolean isGitSubmodule(String path) {
		File rootFolder = new File(path);
		File[] listOfFiles = rootFolder.listFiles();

		boolean isSubmodule = false;
		if (listOfFiles != null) {
			for (int i = 0; i < listOfFiles.length; i++) {
				File child = listOfFiles[i];
        if (!child.isDirectory() 
				    && ".git".equals(child.getName())) {
					isSubmodule = true;
				}
			}
		}
		
		return isSubmodule;
	}

	/**
	 * Search for the project (.xpr) file.
	 * 
	 * @param projectViewPath The directory path to search for.
	 * 
	 * @return the file path to the project file, or <code>null</code>.s
	 */
	public static String findXPR(String projectViewPath) {
		File rootFolder = new File(projectViewPath);  // NOSONAR findsecbugs:PATH_TRAVERSAL_IN
		File[] listOfFiles = rootFolder.listFiles();

		String xprPath = null;
		for (int i = 0; i < listOfFiles.length; i++) {
			String extension = listOfFiles[i].getName().substring(listOfFiles[i].getName().lastIndexOf('.') + 1);
			if ("xpr".equals(extension)) {
				xprPath = listOfFiles[i].getAbsolutePath();
				break;
			}
		}
		return xprPath;
	}

	/**
	 * Check if the given argument is a URL.
	 * 
	 * @param arg the argument.
	 * 
	 * @return <code>true</code> if URL.
	 */
	public static boolean isURL(String arg) {
		try {
			new URL(arg);
			return true;
		} catch (MalformedURLException e) {
			return false;
		}
	}
	
	/**
	 * @param ext The extension to check.
	 * 
	 * @return <code>true</code> if the given text is an archive extension.
	 */
	public static boolean isArchiveExtension(String ext) {
	  return ARCHIVE_EXTENSIONS.contains(ext);
	}
	
	 /**
   * Refresh project view.
   * 
   * @throws NoRepositorySelected
   */
  public static void refreshProjectView() throws NoRepositorySelected {
    StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    ProjectController projectManager = pluginWS.getProjectManager();
    String projectDirPath = pluginWS.getUtilAccess().expandEditorVariables("${pd}", null);
    Repository repository = GitAccess.getInstance().getRepository();
    String repoPath = repository.getDirectory().getParent();
    if (repoPath.startsWith(projectDirPath)) {
      projectManager.refreshFolders(new File[] { new File(repoPath) });  // NOSONAR findsecbugs:PATH_TRAVERSAL_IN
    } else if (projectDirPath.startsWith(repoPath)) {
      projectManager.refreshFolders(new File[] { new File(projectDirPath) });  // NOSONAR findsecbugs:PATH_TRAVERSAL_IN
    }
  }
  
  /**
   * Method used to truncate the given file location according with the given width
   * and considering the font metrics.
   * 
   * @param fileLocation The original file location that needs to be truncated.
   * @param fontMetrics  The font metrics used to compute the string widths.
   * @param maxWidth     The maximum allowed width, in pixels.
   * 
   * @return The truncated text.
   */
  public static String truncateFileLocation(String fileLocation, java.awt.FontMetrics fontMetrics, int maxWidth) {
    if (fileLocation == null || maxWidth <= 0) {
      // Avoid NPEs.
      return fileLocation;
    }
    
    StringBuilder stringBuilder = new StringBuilder();
    if (fontMetrics.stringWidth(fileLocation) <= maxWidth) {
      stringBuilder.append(fileLocation);
    } else {
      String separator = "/";
      if (fileLocation.indexOf('\\') != -1) {
        // There is a Windows like path.
        separator = "\\";
      }
      
      StringTokenizer stk = new StringTokenizer(fileLocation, separator, false);
      List<String> tokens = new ArrayList<>();
      while (stk.hasMoreTokens()) {
        tokens.add(stk.nextToken());
      }
      
      if (tokens.size() <= 2) {
        stringBuilder.append(fileLocation);
      } else {
        boolean startsWithSeparator = fileLocation.indexOf(separator) == 0;
        truncateFileLocation(fontMetrics, maxWidth, stringBuilder, separator, startsWithSeparator, tokens);
      }
    }
    
    return stringBuilder.toString();
  }

  /**
   * Truncate file location.
   * 
   * @param fontMetrics         The font metrics used to compute the string widths.
   * @param maxWidth            maximum allowed width. 
   * @param stringBuilder       string builder used to build the truncated location.
   * @param separator           the separator.
   * @param startsWithSeparator <code>true</code> if the location starts with a separator.
   * @param tokens              tokens originally separated by the given separator.
   */
  private static void truncateFileLocation(
      java.awt.FontMetrics fontMetrics,
      int maxWidth,
      StringBuilder stringBuilder,
      String separator,
      boolean startsWithSeparator,
      List<String> tokens) {
    if (startsWithSeparator) {
      stringBuilder.append(separator);
    }
    stringBuilder.append(tokens.get(0));
    stringBuilder.append(separator);
    stringBuilder.append("...");
    
    StringBuilder auxStringBuilder = new StringBuilder();
    int noOfTokens = tokens.size();
    for (int i = noOfTokens  - 1; i >= 2; i--) {
      String token = tokens.get(i);
      if (fontMetrics.stringWidth(stringBuilder.toString() + auxStringBuilder.toString() + token) < maxWidth) {
        auxStringBuilder.insert(0, token);
        auxStringBuilder.insert(0, separator);
      } else {
        break;
      }
    }
    
    if (auxStringBuilder.length() == 0) {
      stringBuilder.append(separator);
      int aproxNumberOfCharsInWidth = maxWidth / fontMetrics.charWidth('w');
      stringBuilder.append(getSomeTextAtEnd(
          tokens.get(noOfTokens - 1), 
          Math.max(aproxNumberOfCharsInWidth - stringBuilder.length(), 0)));
    } else {
      stringBuilder.append(auxStringBuilder);
    }
  }
  
  /**
   * Get some text from the end of the string. It adds &quot;...&quot; when string limit is exceeded.
   * This is used when the file name is too large to be displayed as it is.
   * 
   * @param str The original string.
   * @param maxLen The max length where to trim the string.
   * 
   * @return The trimmed text.
   */
  public static String getSomeTextAtEnd(String str, int maxLen) {
    int strLen = str != null ? str.length() : 0;
    if (str != null && strLen > maxLen) {
      str = str.substring(strLen - maxLen, strLen);
      String threeDots = "...";
      int threeDotsLength = threeDots.length();
      if (str.length() > threeDotsLength) {
        str = threeDots + str.substring(threeDotsLength);
      }
    }
    return str;
  }
  
  /**
   * Checks if we have just one resource and if it's a resource that is committed in the repository.
   *
   * @param allSelectedResources A set of resources.
   *
   * @return <code>true</code> if we have just one resource in the set and that resource is one with history.
   */
  public static boolean shouldEnableBlameAndHistory(final List<FileStatus> allSelectedResources) {
    boolean hasHistory = false;
    if (allSelectedResources.size() == 1) {
      GitChangeType changeType = allSelectedResources.get(0).getChangeType();
      hasHistory =
              changeType == GitChangeType.CHANGED ||
                      changeType == GitChangeType.CONFLICT ||
                      changeType == GitChangeType.MODIFIED;
    }
    return hasHistory;
  }

  /**
   * Check if there is at least a file in the given collection that contains conflict markers.
   *
   * @param allSelectedResources the files.
   * @param workingCopy          the working copy.
   *
   * @return <code>true</code> if there's at least a file that contains at least a conflict marker.
   */
  public static boolean containsConflictMarkers(
          final List<FileStatus> allSelectedResources,
          final File workingCopy) {
    return allSelectedResources.stream()
            .parallel()
            .anyMatch(file -> containsConflictMarkers(file, workingCopy));
  }

  /**
   * Check if a file contains conflict markers.
   *
   * @param fileStatus  the file status.
   * @param workingCopy the working copy.
   *
   * @return <code>True</code> if the file contains at least a conflict marker.
   */
  private static boolean containsConflictMarkers(
          final FileStatus fileStatus,
          final File workingCopy) {
    boolean toReturn = false;
    UtilAccess utilAccess = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess();
    File currentFile = new File(workingCopy, FilenameUtils.getName(fileStatus.getFileLocation())); // NOSONAR findsecbugs:PATH_TRAVERSAL_IN
    try (BufferedReader reader =  new BufferedReader(utilAccess.createReader(currentFile.toURI().toURL(), StandardCharsets.UTF_8.toString()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains("<<<<<<<") || line.contains("=======") || line.contains(">>>>>>>")) {
          toReturn = true;
          break;
        }
      }
    } catch (IOException ex) {
      LOGGER.error(ex.getMessage(), ex);
    }
    return toReturn;
  }
  
  /**
   * Get canonical path.
   * 
   * @param file A file. 
   * 
   * @return The canonical version of the file.
   */
  public static String getCanonicalPath(final File file) {
    String repoPath;
    try {
      repoPath = file.getCanonicalPath();
    } catch (IOException e) {
      LOGGER.debug(e.getMessage(), e);
      repoPath = file.getAbsolutePath();
    }
    return repoPath;
  }
  
  /**
   * Checks if the two files are equal.
   * 
   * @param first The first file.
   * @param second The second file.
   * 
   * @return <code>true</code> if the files have the same paths.
   */
  public static boolean same(File first, File second) {
    boolean same = false;

    try {
      first = first.getCanonicalFile();
      second = second.getCanonicalFile();

      same = first.equals(second); 
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }

    return same;
  }
  
  /**
   * Search a file by an extension.
   * 
   * @param file       The directory to search. 
   * @param extension  The file extension.
   * 
   * @return The first file found with the given extension.
   */
  @Nullable public static File searchFileByExtension(final File file, final String extension) {
    File fileToReturn = null;
    if (file.isDirectory()) {
      File[] arr = file.listFiles();
      for (File f : arr) {
        File found = searchFileByExtension(f, extension);
        if (found != null) {
          fileToReturn = found;
          break;
        }
      }
    } else {
      if (file.getName().endsWith(extension)) {
        fileToReturn = file;
      }
    }
    return fileToReturn;
  }
  
  /**
   * Search recursively all the files by the given extension
   * 
   * @param wcDir The directory to search
   * 
   * @return A list of all the files with the given extension
   */
  @NonNull
  public static List<File> findAllFilesByExtension(File wcDir, String extension) {
    return findAllFilesByExtension(wcDir, new ArrayList<>(), extension);
  }
  
  /**
   * Search all the files by the given extension and add them to the given list
   * 
   * @param startDir    The directory to search recursively
   * @param foundFiles  The list with the found files
   *  
   * @return A list of all the files with the given extension found in the given directory
   */
  private static List<File> findAllFilesByExtension(File startDir, List<File> foundFiles, String extension) {
    for (File file : startDir.listFiles(pathname -> !pathname.isHidden())) {
      if(file.getName().endsWith(extension)) {
        foundFiles.add(file);
      }
      if (file.isDirectory()) {
        findAllFilesByExtension(file, foundFiles, extension);
      }
    }
    return foundFiles;
  }
  
  /**
   * Checks if the given URL is for a local file.
   * 
   * @param url The URL to be checked.
   * 
   * @return <code>true</code> if the URL is for file.
   */
  public static boolean isURLForLocalFile(final URL url) {
    return PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().locateFile(url) != null;
  }
  
  /**
   *  Deletes recursively the specified directory.
   * 
   *  @param  file   The file or directory to be deleted.
   *  
   *  @throws IOException When cannot delete a resource.
   */
  public static void deleteRecursivelly(File file) throws IOException {
    if (!file.exists()) {
      // Nothing to do.
      return;
    }
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      if (files != null) {
        for (int i = 0; i < files.length; i++) {
          deleteRecursivelly(files[i]);
        }
      }
      Files.delete(file.toPath());
    } else {
      Files.delete(file.toPath());
    }
  }
  
}
