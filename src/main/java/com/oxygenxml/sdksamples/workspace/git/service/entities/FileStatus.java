package com.oxygenxml.sdksamples.workspace.git.service.entities;

public class FileStatus {
	
	public static enum StageState {
		STAGED, UNSTAGED
	}
	/**
	 * 
	 */
	private String changeType;
	private String fileLocation;

	public FileStatus(String changeType, String fileLocation) {
		this.changeType = changeType;
		this.fileLocation = fileLocation;
	}

	public String getChangeType() {
		return changeType;
	}

	public void setChangeType(String changeType) {
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
