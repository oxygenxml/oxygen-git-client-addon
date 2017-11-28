package com.oxygenxml.git.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jgit.errors.NoWorkTreeException;

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
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(FileHelper.class);

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
		List<String> fileNames = new ArrayList<String>();
		if (rootFolder.isFile()) {
			fileNames.add(rootFolder.getAbsolutePath().replace("\\", "/"));
			return fileNames;
		}
		File[] listOfFiles = rootFolder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isDirectory() && (!listOfFiles[i].getName().equals(".git"))) {
				fileNames.addAll(getAllFilesFromPath(listOfFiles[i].getAbsolutePath()));
			} else if (listOfFiles[i].isFile()) {
				fileNames.add(listOfFiles[i].getAbsolutePath().replace("\\", "/"));
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
	 * @throws NoWorkTreeException 
	 */
	public static URL getFileURL(String path) throws NoWorkTreeException, NoRepositorySelected {
		String selectedRepository = GitAccess.getInstance().getWorkingCopy().getAbsolutePath();
		selectedRepository = selectedRepository.replace("\\", "/");
		URL url = null;
		File file = new File(selectedRepository + "/" + path);
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
			String extension = listOfFiles[i].getName().substring(listOfFiles[i].getName().lastIndexOf(".") + 1);
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
}
