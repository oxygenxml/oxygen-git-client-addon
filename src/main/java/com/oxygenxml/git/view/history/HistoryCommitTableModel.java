package com.oxygenxml.git.view.history;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.Equaler;


/**
 * Table Model for Commit History Characteristics.
 * 
 * @Alexandra_Dinisor
 *
 */
@SuppressWarnings("serial")
public class HistoryCommitTableModel extends AbstractTableModel {

	/*
	 * Constants for the index representing the table column.
	 */
	public static final int COMMIT_MESSAGE = 0;
	public static final int DATE = 1;
	public static final int AUTHOR = 2;
	public static final int COMMIT_ABBREVIATED_ID = 3;
	
	/**
   * Text from filter field
   */
  private String textToFilter = "";

	/**
	 * The internal representation of the model.
	 */
	private transient List<CommitCharacteristics> allCommitsCharacteristics;
	
	/**
   * The internal representation of the model filtered.
   */
  private transient List<CommitCharacteristics> allCommitsCharacteristicsFiltered;

	/**
	 * Construct the Table Model with a Vector containing all commitCharacteristics.
	 * 
	 * @param commitVector The computed commitVector
	 */
	public HistoryCommitTableModel(List<CommitCharacteristics> commitVector) {
		this.allCommitsCharacteristicsFiltered = new ArrayList<>(commitVector);
		this.allCommitsCharacteristics = new ArrayList<>(commitVector);
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		CommitCharacteristics commitCharacteristics = allCommitsCharacteristicsFiltered.get(rowIndex);
		Object temp = null;

		switch (columnIndex) {
		case COMMIT_MESSAGE:
			temp = commitCharacteristics;
			break;
		case AUTHOR:
			temp = commitCharacteristics.getAuthor();
			break;
		case DATE:
			temp = commitCharacteristics.getDate();
			break;
		case COMMIT_ABBREVIATED_ID:
			temp = commitCharacteristics.getCommitAbbreviatedId();
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
		case COMMIT_MESSAGE:
			clazz = CommitCharacteristics.class;
			break;
		case DATE:
			clazz = Date.class;
			break;
		case AUTHOR:
			clazz = String.class;
			break;
		case COMMIT_ABBREVIATED_ID:
			clazz = String.class;
			break;
		default:
			break;
		}
		return clazz;
	}
	
	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}
	
	@Override
	public int getRowCount() {
		return allCommitsCharacteristicsFiltered != null ? allCommitsCharacteristicsFiltered.size() : 0;
	}

	@Override
	public int getColumnCount() {
		return 4;
	}
	
	@Override
	public String getColumnName(int index) {
		String columnName = null;
		switch (index) {
		case COMMIT_MESSAGE:
			columnName = Translator.getInstance().getTranslation(Tags.COMMIT_MESSAGE_LABEL);
			break;
		case DATE:
			columnName = Translator.getInstance().getTranslation(Tags.DATE);
			break;
		case AUTHOR:
			columnName = Translator.getInstance().getTranslation(Tags.AUTHOR);
			break;
		case COMMIT_ABBREVIATED_ID:
			columnName = Translator.getInstance().getTranslation(Tags.COMMIT);
			break;
		default:
			break;
		}		
		return columnName;	
	}
	
	public List<CommitCharacteristics> getAllCommits() {
    return allCommitsCharacteristicsFiltered;
  }
	
	/**
	 * Filters the table
	 * @param text The text to filter
	 */
	public void filterChanged(String textFilter) {
	  if (!Equaler.verifyEquals(textFilter, this.textToFilter)) {
	    this.textToFilter = textFilter;
	    allCommitsCharacteristicsFiltered.clear();
	    if (textFilter != null && textFilter.length() > 0) {
	      for (Iterator<CommitCharacteristics> iterator = allCommitsCharacteristics.iterator(); iterator.hasNext();) {
	        CommitCharacteristics comitCharac = iterator.next();
	        if(!shouldFilter(comitCharac, textToFilter)) {
	          allCommitsCharacteristicsFiltered.add(comitCharac);
	        }
	      }
	    }
	    //update model
	    fireTableDataChanged();
	  }
	}
	
	/**
	 * Tells if a commit should be removed or not.
	 * 
	 * @param commitCharac The commit with details
	 * @param textFilter The filter that should be applied
	 * 
	 * @return True if the commit should be removed, false otherwise
	 */
	private static boolean shouldFilter(CommitCharacteristics commitCharac, String textFilter) {
    if( textFilter != null &&  !textFilter.isEmpty()) {
      String date = "";
      String author = ""; 
      String commitId = "";
      String message = "";
      
      String authorTemp = commitCharac.getAuthor();
      if(authorTemp != null) {
        author = authorTemp.toLowerCase();
      }
      Date dateTemp = commitCharac.getDate();
      if(dateTemp != null) {
        date = dateTemp.toString();
      }
      String commitIdTemp = commitCharac.getCommitId();
      if(commitIdTemp != null) {
        commitId = commitIdTemp.toLowerCase();
      }
      String messageTemp = commitCharac.getCommitMessage();
      if(messageTemp != null) {
        message = messageTemp.toLowerCase();
      }
      ///////text filter
      String[] tokens = textFilter.split("[, .!-]+");
      for (int i = 0; i < tokens.length; i++) {
        String valueTerm = tokens[i].trim().toLowerCase();
        String valueDate = tokens[i].trim();
        if(!author.contains(valueTerm) && !commitId.contains(valueTerm) &&
            !date.contains(valueDate) && !message.contains(valueTerm)){
          return true;
        }
      }
    } 
    return false;
  }

}
