package com.oxygenxml.sdksamples.workspace.git.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Folder {

	private String prefixPath;
	
	public Folder(){
		this.prefixPath = "";
	}
	
	public List<String> search(String path){
		File rootFolder = new File(path);
		File[] listOfFiles = rootFolder.listFiles();

		String tempPrefixPath = prefixPath;
		prefixPath += rootFolder.getName() + "/";
		
		List<String> fileNames = new ArrayList<String>();

		for (int i = 0; i < listOfFiles.length; i++) {
			if(listOfFiles[i].isDirectory()){
				fileNames.addAll(search(listOfFiles[i].getAbsolutePath()));
			} else if (listOfFiles[i].isFile()){
				fileNames.add(prefixPath + listOfFiles[i].getName());
			}
		}
		prefixPath = tempPrefixPath;
		return fileNames;
	}
	
}
