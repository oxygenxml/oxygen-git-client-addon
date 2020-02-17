package com.oxygenxml.git.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;

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
      }}
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
		File rootFolder = new File(path);
		File[] listOfFiles = rootFolder.listFiles();

		if (listOfFiles != null) {
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isDirectory() && ".git".equals(listOfFiles[i].getName())) {
					return true;
				}
			}
		}
		return false;
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
	 * @return <code>true</code> if the given text is an archive extension.
	 */
	public static boolean isArchiveExtension(String ext) {
	  return ARCHIVE_EXTENSIONS.contains(ext);
	}
}
