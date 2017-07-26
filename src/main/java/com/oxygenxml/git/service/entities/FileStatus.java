package com.oxygenxml.git.service.entities;

import org.eclipse.jgit.diff.DiffEntry.ChangeType;

/**
 * Git File Status. Used to store the file location and the file state(DELETED,
 * ADDED, MODIFIED)
 * 
 * @author intern2
 *
 */
public class FileStatus {
	
	/**
	 * A file can be Added, Deleted or Modified
	 */
	private GitChangeType changeType;
	
	/**
	 * The file location is releative to the selected git repository. For example
	 * if a a git Repository is in C:/Git and we have a file stored in
	 * C:/Git/file.txt. The fileLocation will be file.txt. If we have a file
	 * sotred in C:/Git/Folder/file.txt, the fileLocation will be Folder/file.txt
	 */
	private String fileLocation;

	public FileStatus(GitChangeType changeType, String fileLocation) {
		this.changeType = changeType;
		this.fileLocation = fileLocation;
	}

	public FileStatus(FileStatus fileStatus) {
		this.changeType = fileStatus.getChangeType();
		this.fileLocation = fileStatus.getFileLocation();
	}

	public GitChangeType getChangeType() {
		return changeType;
	}

	public void setChangeType(GitChangeType changeType) {
		this.changeType = changeType;
	}

	public String getFileLocation() {
		return fileLocation;
	}

	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}

	@Override
	public String toString() {
		return "UnstageFile [changeType=" + changeType + ", fileLocation=" + fileLocation + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changeType == null) ? 0 : changeType.hashCode());
		result = prime * result + ((fileLocation == null) ? 0 : fileLocation.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileStatus other = (FileStatus) obj;
		if (changeType == null) {
			if (other.changeType != null)
				return false;
		} else if (!changeType.equals(other.changeType))
			return false;
		if (fileLocation == null) {
			if (other.fileLocation != null)
				return false;
		} else if (!fileLocation.equals(other.fileLocation))
			return false;
		return true;
	}

}
