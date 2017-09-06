package com.oxygenxml.git.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.oxygenxml.git.options.OptionsManager;

/**
 * An utility class for files
 * 
 * @author Beniamin Savu
 *
 */
public class FileHelper {

	/**
	 * Searches a given path for all files in that path. Generates the files path
	 * relative to the given path and saves them in a list
	 * 
	 * @param path
	 *          - the path to the folder you want to search
	 * @return list of paths relative to the given path
	 */
	public static List<String> search(String path) {
		File rootFolder = new File(path);
		List<String> fileNames = new ArrayList<String>();
		if(rootFolder.isFile()){
			fileNames.add(rootFolder.getAbsolutePath().replace("\\", "/"));
			return fileNames;
		}
		File[] listOfFiles = rootFolder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isDirectory() && (!listOfFiles[i].getName().equals(".git"))) {
				fileNames.addAll(search(listOfFiles[i].getAbsolutePath()));
			} else if (listOfFiles[i].isFile()) {
				fileNames.add(listOfFiles[i].getAbsolutePath().replace("\\", "/"));
			}
		}
		return fileNames;
	}

	/**
	 * Returns the URL from a given path
	 * 
	 * @param path
	 *          - the path to get the URL
	 * @return the URL from the given path
	 */
	public static URL getFileURL(String path) {

		String selectedRepository = OptionsManager.getInstance().getSelectedRepository();
		selectedRepository = selectedRepository.replace("\\", "/");
		URL url = null;
		File file = new File(selectedRepository + "/" + path);
		try {
			url = file.toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return url;
	}

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
	
	public static boolean isGitSubmodule(String path){
		File rootFolder = new File(path);
		File[] listOfFiles = rootFolder.listFiles();

		if (listOfFiles != null) {
			for (int i = 0; i < listOfFiles.length; i++) {
				if (!listOfFiles[i].isDirectory() && ".git".equals(listOfFiles[i].getName())) {
					return true;
				}
			}
		}
		return false;
	}

	public static String findXPR(String projectViewPath) {
		File rootFolder = new File(projectViewPath);
		File[] listOfFiles = rootFolder.listFiles();

		String xprPath = "";
		for (int i = 0; i < listOfFiles.length; i++) {
			String extension = listOfFiles[i].getName().substring(listOfFiles[i].getName().lastIndexOf(".") + 1);
			if ("xpr".equals(extension)) {
				xprPath = listOfFiles[i].getAbsolutePath();
				break;
			}
		}
		return xprPath;
	}

	public static boolean isURL(String path) {
		try {
			new URL(path);
			return true;
		} catch (MalformedURLException e) {
			return false;
		}
	}
}
