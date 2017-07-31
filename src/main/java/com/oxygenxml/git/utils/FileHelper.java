package com.oxygenxml.git.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An utility class for files
 * 
 * @author Beniamin Savu
 *
 */
public class FileHelper {

	/**
	 * Saves the upper path from the current path. For Example having the current
	 * path "C:/test/folder1/folder2", the prefixPath will be "C:/test/folder1"
     * TODO Statics are bad! Let's pass this path as a parameter.
	 */
	private static String prefixPath = "";

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
		File[] listOfFiles = rootFolder.listFiles();

		String tempPrefixPath = prefixPath;
		prefixPath += rootFolder.getName() + "/";

		List<String> fileNames = new ArrayList<String>();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isDirectory() && (!listOfFiles[i].getName().equals(".git"))) {
				fileNames.addAll(search(listOfFiles[i].getAbsolutePath()));
			} else if (listOfFiles[i].isFile()) {
				fileNames.add(prefixPath + listOfFiles[i].getName());
			}
		}
		prefixPath = tempPrefixPath;
		return fileNames;
	}
}
