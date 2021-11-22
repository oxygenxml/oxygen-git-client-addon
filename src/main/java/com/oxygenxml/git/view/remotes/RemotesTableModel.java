package com.oxygenxml.git.view.remotes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

/**
 * Model for remotes of repository.
 * 
 * @author alex_smarandache
 *
 */
public class RemotesTableModel extends AbstractTableModel {

	  /**
	   * Index of the remote name column.
	   */
	  public static final int REMOTE_COLUMN = 0;

	  /**
	   * Index of the remote url column.
	   */
	  public static final int URL_COLUMN = 1;

	  /**
	   * Constant for number of columns.
	   */
	  public static final int NO_OF_COLUMNS = 2;
	  
	  /**
	   * The columns names.
	   */
	  private static final String[] COLUMNS_NAMES = new String[]{
	    Translator.getInstance().getTranslation(Tags.NAME),
	    Translator.getInstance().getTranslation(Tags.URL),
	  };

	  /**
	   * The internal representation of the model.
	   */
	  private transient List<Remote> remotes = new ArrayList();
	  
	  
	  
	  /**
	   * Class for remote.
	   * 
	   * @author alex_smarandache
	   *
	   */
	   class Remote {
		  
		  /**
		   * Remote name.
		   */
		  String remoteName;
		  
		  /**
		   * Remote URL.
		   */
		  String url;
		  
		  
		  /**
		   * Constructor.
		   * 
		   * @param remoteName   Remote name.
		   * @param url          Remote URL.
		   */
		  public Remote(String remoteName, String url) {
			  this.remoteName = remoteName;
			  this.url = url;
		  }
		  
	  }
	  
	  
	   
	  @Override
	  public boolean isCellEditable(int row, int column) {
	    return false;
	  }


	  @Override
	  public int getRowCount() {
	    return remotes.size();
	  }
	  
	  
	  @Override
	  public int getColumnCount() {
	    return NO_OF_COLUMNS;
	  }

	  @Override
	  public Object getValueAt(int rowIndex, int columnIndex) {
	    Object temp = null;
	    switch (columnIndex) {
	      case REMOTE_COLUMN:
	        temp = remotes.get(rowIndex).remoteName;
	        break;
	      case URL_COLUMN:
	        temp = remotes.get(rowIndex).url;
	        break;
	      default:
	        break;
	    }

	    return temp;
	  }
	  
	  @Override
	  public Class<?> getColumnClass(int columnIndex) {
	    Class<?> clazz = null;
	    
	    switch (columnIndex) {
	      case REMOTE_COLUMN: 
	      case URL_COLUMN:
	        clazz = String.class;
	        break;
	      default:
	        break;
	    }
	    
	    return clazz;
	  }

	  
	  @Override
	  public String getColumnName(int col) {
	    return COLUMNS_NAMES[col];
	  }
	  

	  /**
	   * Removes all rows.
	   */
	  public void clear() {
	    int size = remotes.size();
	    remotes.clear();
	    fireTableRowsDeleted(0, size - 1);
	  }

	  
      /**
       * Update the remotes from current repository.
       * 
       * @param remotesMap <code>key</code>: the remote name, <code>value</code>: the remote URL.
       */
	  public void setRemotes(Map<String, String> remotesMap) {
		  clear();
		  remotesMap.keySet().forEach(remote -> remotes.add(new Remote(remote, remotesMap.get(remote))));
		  fireTableRowsDeleted(0, remotes.size() - 1);
	  }
	  
	  
	  /**
	   * Edit the remote values.
	   * 
	   * @param index    The Index of remote.
	   * @param newName  The new name of remote.
	   * @param newURL   The new URL of remote.
	   */
	  public void editRemote(int index, String newName, String newURL) {
		  remotes.set(index, new Remote(newName, newURL));
		  fireTableRowsUpdated(index, index);
	  }
	  
	  
	  /**
	   * Edit the remote values.
	   * 
	   * @param oldName  The oldName of remote.
	   * @param newName  The new name of remote.
	   * @param newURL   The new URL of remote.
	   */
	  public void editRemote(String oldName, String newName, String newURL) {
		  for(int i = 0; i < remotes.size(); i++) {
			  if(remotes.get(i).remoteName.equals(oldName)) {
				  editRemote(i, newName, newURL);
				  break;
			  }
		  }
	  }
	  
	  
	  /**
	   * Delete a remote.
	   * 
	   * @param index    The Index of remote.
	   */
	  public void deleteRemote(int index) {
		  remotes.remove(index);
		  fireTableRowsDeleted(index, index);
	  }
	  
	  
	  /**
	   * Add a remote.
	   * 
	   * @param name  The name of remote.
	   * @param URL   The URL of remote.
	   */
	  public void addRemote(String name, String URL) {
		  remotes.add(new Remote(name, URL));
		  fireTableRowsInserted(remotes.size() -1 , remotes.size() -1);
	  }
	  
	  
	  /**
	   * @param remoteName  The remote name
	   * 
	   * @return <code>true</code> if the remote already exists.
	   */
	  public boolean remoteAlreadyExists(String remoteName) {
		  return remotes.stream().anyMatch(remote -> remote.remoteName.equals(remoteName));
	  }
	   
}
