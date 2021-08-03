package com.oxygenxml.git.utils;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Repository;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;

/**
 * An utility class for files
 * 
 * @author Beniamin Savu
 *
 */
public class FileHelper {
  
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
  private FileHelper() {
    // Nothing
  }

	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(FileHelper.class);
	
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
    File commonAncestor = null;
    
    if (dirs != null && !dirs.isEmpty()) {
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
      
      commonAncestor = new File(commonPathBuilder.toString());
    }
    
    return commonAncestor;
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
		File rootFolder = new File(path);
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
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
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
    selectedRepositoryPath = FileHelper.rewriteSeparator(selectedRepositoryPath);

    String fileInWorkPath = FileHelper.rewriteSeparator(file.getAbsolutePath());
    if (fileInWorkPath.startsWith(selectedRepositoryPath)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Notify " + fileInWorkPath);
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
      if (FileHelper.isGitRepository(temp.getPath())) {
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
		File rootFolder = new File(projectViewPath);
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
      projectManager.refreshFolders(new File[] { new File(repoPath) });
    } else if (projectDirPath.startsWith(repoPath)) {
      projectManager.refreshFolders(new File[] { new File(projectDirPath) });
    }
  }
  
  /**
   * Method used to truncate the given text according with the given width
   * and considering the font metrics.
   * 
   * @param text The original text that needs to be truncated.
   * @param fontMetrics The font metrics used to compute the string widths.
   * @param width The maximum allowed width, in pixels.
   * @return The truncated text.
   */
  public static String truncateText(String text, java.awt.FontMetrics fontMetrics, int width) {
    if (text == null || width <= 0) {
      // Avoid NPEs.
      return text;
    }
    StringBuilder buf = new StringBuilder();
    if (fontMetrics.stringWidth(text) <= width) {
      buf.append(text);
    } else {
      String separator = "/";
      if (text.indexOf('\\') != -1) {
        // There is a Windows like path.
        separator = "\\";
      }
      
      boolean startsWithSeparator = text.indexOf(separator) == 0;
      
      StringTokenizer stk = new StringTokenizer(text, separator, false);
      List<String> tokens = new ArrayList<>();
      while (stk.hasMoreTokens()) {
        tokens.add(stk.nextToken());
      }
      int maxTokens = tokens.size();
      
      if (maxTokens <= 2) {
        buf.append(text);
      } else {
        if (startsWithSeparator) {
          buf.append(separator);
        }
        buf.append(tokens.get(0));
        buf.append(separator);
        buf.append("...");
        
        StringBuilder secondBuf = new StringBuilder();
        
        for (int i = maxTokens - 1; i >= 2; i--) {
          String token = tokens.get(i);
          if (fontMetrics.stringWidth(buf.toString() + secondBuf.toString() + token) < width) {
            secondBuf.insert(0, token);
            secondBuf.insert(0, separator);
          } else {
            break;
          }
        }
        
        if (secondBuf.length() == 0) {
          buf.append(separator);
          int aproxNumberOfCharsInWidth = width / fontMetrics.charWidth('w');
          buf.append(getSomeTextAtEnd(
              tokens.get(maxTokens - 1), 
              Math.max(aproxNumberOfCharsInWidth - buf.length(), 0)));
        } else {
          buf.append(secondBuf);
        }
      }
    }
    
    return buf.toString();
  }
  
  /**
   * Get some text from the end of the string. It adds &quot;...&quot; when string limit is exceeded.
   * This is used when the file name is too large to be displayed as it is.
   * 
   * @param str The original string.
   * @param maxLen The max length where to trim the string.
   * @return The trimmed text.
   */
  public static String getSomeTextAtEnd(String str, int maxLen) {
    int strLen = str != null ? str.length() : 0;
    if (str != null && strLen > maxLen) {
      str = str.substring(strLen - maxLen, strLen);
      if (str.length() > 3) {
        str = "..." + str.substring(3);
      }
    }
    return str;
  }
  
}
