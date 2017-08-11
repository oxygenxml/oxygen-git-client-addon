package com.oxygenxml.git.service.entities;

import java.util.Comparator;

public class FileStatusComparator implements Comparator<FileStatus>{

	public int compare(FileStatus f1, FileStatus f2) {
		int changeTypeCompareResult = f1.getChangeType().compareTo(f2.getChangeType());
		if(changeTypeCompareResult == 0){
			String fileName1 = f1.getFileLocation().substring(f1.getFileLocation().lastIndexOf("/") + 1);
			String fileName2 = f2.getFileLocation().substring(f2.getFileLocation().lastIndexOf("/") + 1);
			return fileName1.compareTo(fileName2);
		} else {
			return changeTypeCompareResult;
		}
	}

}
