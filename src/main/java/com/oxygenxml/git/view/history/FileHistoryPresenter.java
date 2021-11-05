package com.oxygenxml.git.view.history;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for providing useful information about the files presented.
 * 
 * @author alex_smarandache
 *
 */
public class FileHistoryPresenter {
	
	/**
	 * Current presented file path.
	 */
	private String filePath;
	
	/**
	 * List with observers.
	 */
	private final List<IObserver> observers;
	
	
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
	     observers = new ArrayList<>();
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
		notifyObservers();
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
	
	/**
	 * Notify all observers.
	 */
	private void notifyObservers() {
		for(IObserver iterator: observers) {
			iterator.update();
		}
	}
	
	/**
	 * Add a new observer to list.
	 * 
	 * @param observer observer to be added.
	 */
	public void addObserver(IObserver observer) {
		observers.add(observer);
	}

}
