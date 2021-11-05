package com.oxygenxml.git.view.history;

/**
 * Responsible for providing useful information about the files presented.
 * 
 * @author alex_smarandache
 *
 */
public class FileHistoryPresenter {
	
	// TODO maybe use Observer design pattern to notity about presented file path changes.
	
	/**
	 * Current presented file path.
	 */
	private String filePath;
	
	
	/**
	 * Constructor.
	 * 
	 * The file presented are null value by default.
	 */
	public FileHistoryPresenter() {
	     this(null);
	}
	
	
	/**
	 * Constructor.
	 * 
	 * @param filePath File path to present.
	 */
	public FileHistoryPresenter(String filePath) {
	     this.filePath = filePath;
	}

	
	/**
	 * @return The current file path presented.
	 */
	public String getFilePath() {
		return filePath;
	}

	
	/**
	 * Update the file path.
	 * 
	 * @param filePath The new file path presented.
	 */
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	
	/**
	 * @return <code>true</code> if is a file history presented.
	 */
	public boolean isFilePresented() {
		return filePath != null;
	}

	
	/**
	 * @param path Path to compare with presented path.
	 * 
	 * @return <code>true</code> if the path are equal or a file of presented path if the file presented is a folder.
	 */
	public boolean isCurrentPathPresented(String path) {
		 return (filePath instanceof String && path instanceof String &&
		          !(filePath.equals(path) || path.startsWith(filePath + "/", 0)) 
				 );
	}
}
