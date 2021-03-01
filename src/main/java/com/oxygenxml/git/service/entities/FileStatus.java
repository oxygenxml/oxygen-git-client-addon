package com.oxygenxml.git.service.entities;

import com.oxygenxml.git.utils.Equaler;

/**
 * Git File Status. Used to store the file location and the file state(DELETED,
 * ADDED, MODIFIED)
 * 
 * @author Beniamin Savu
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
	/**
	 * An optional description for the change suffered by the resource. For example, a submodule change will give details 
	 * as the remote repository it is bound to, information about the previous and the new revision (maybe date and author).
	 */
	private String description;

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

	@Override
	public String toString() {
		return "(changeType=" + changeType + ", fileLocation=" + fileLocation + ")";
	}

	@Override
	public int hashCode() {
		return fileLocation.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
	  boolean equals = false;
	  if (obj instanceof FileStatus) {
	    FileStatus other = (FileStatus) obj;
	    equals = 
	        (other.changeType == GitChangeType.UNKNOWN 
	        || changeType == GitChangeType.UNKNOWN 
	        || other.changeType == changeType) 
          &&  Equaler.verifyEquals(other.fileLocation, fileLocation);
	  }
	  return equals;
	}

	/**
	 * Set description.
	 * 
	 * @param description An optional description for the change suffered by the resource. For example, a submodule change will give details 
   * as the remote repository it is bound to, information about the previous and the new revision (maybe date and author).
   * 
   * @return the current file status, which now has a new description.
	 */
	public FileStatus setDescription(String description) {
    this.description = description;
    
    return this;
  }
	
	/**
	 * @return An optional description for the change suffered by the resource. For example, a submodule change will give details 
   * as the remote repository it is bound to, information about the previous and the new revision (maybe date and author).
	 */
	public String getDescription() {
    return description;
  }
}
