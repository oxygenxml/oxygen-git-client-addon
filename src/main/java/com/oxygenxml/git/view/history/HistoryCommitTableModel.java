package com.oxygenxml.git.view.history;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.eclipse.jgit.revplot.PlotCommit;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.Equaler;


/**
 * Table Model for Commit History Characteristics.
 * 
 * @Alexandra_Dinisor
 *
 */
public class HistoryCommitTableModel extends AbstractTableModel {
  /**
   * Number of columns.
   */
  private static final int COLUMN_COUNT = 5;
  
  /**
   * Length of the short commit id
   */
  private static final int SHORT_COMMIT_ID_LENGTH = 7;
  
  /**
	 * Commit graph table column index.
	 */
	public static final int COMMIT_GRAPH = 0;
  
  /**
	 * Commit message table column index.
	 */
	public static final int COMMIT_MESSAGE = 1;
	
	/**
   *Date table column index.
   */
	public static final int DATE = 2;
	
	/**
   * Author table column index.
   */
	public static final int AUTHOR = 3;
	
	/**
   * Commit ID table column index.
   */
	public static final int COMMIT_ABBREVIATED_ID = 4;
	
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
	
	/**
	 * @see javax.swing.table.TableModel.getValueAt(int rowIndex, int columnIndex)
	 */
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		CommitCharacteristics commitCharacteristics = allCommitsCharacteristicsFiltered.get(rowIndex);
		Object temp = null;

		switch (columnIndex) {
		case COMMIT_GRAPH:
		     temp = commitCharacteristics.getPlotCommit();
		     break;
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

	/**
	 * @see javax.swing.table.AbstractTableModel.getColumnClass(int columnIndex)
	 */
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		Class<?> clazz = null;
		switch (columnIndex) {
		case COMMIT_GRAPH:
			clazz = PlotCommit.class;
			break;
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
	
	/**
	 * @see javax.swing.table.AbstractTableModel.isCellEditable(int rowIndex, int columnIndex)
	 */
	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}
	
	/**
	 * @see javax.swing.table.TableModel.getRowCount()
	 */
	@Override
	public int getRowCount() {
		return allCommitsCharacteristicsFiltered != null ? allCommitsCharacteristicsFiltered.size() : 0;
	}

	/**
	 * @see vax.swing.table.TableModel.getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		return COLUMN_COUNT;
	}
	
	/**
	 * @see javax.swing.table.AbstractTableModel.getColumnName(int column)
	 */
	@Override
	public String getColumnName(int index) {
		String columnName = null;
		switch (index) {
		case COMMIT_GRAPH:
			columnName = Translator.getInstance().getTranslation(Tags.GRAPH);
			break;
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
	
	/**
	 * @return all commits.
	 */
	public List<CommitCharacteristics> getAllCommits() {
    return allCommitsCharacteristicsFiltered;
  }
	
	/**
	 * Filters the table
	 * 
	 * @param text The text to user for filtering.
	 */
	public void filterChanged(String text) {
	  if (!Equaler.verifyEquals(text, this.textToFilter)) {
	    this.textToFilter = text;
	    if (text != null && text.length() > 0) {
	      allCommitsCharacteristicsFiltered.clear();
	      for (Iterator<CommitCharacteristics> iterator = allCommitsCharacteristics.iterator(); iterator.hasNext();) {
	        CommitCharacteristics comitCharac = iterator.next();
	        if(!shouldFilter(comitCharac, textToFilter)) {
	          allCommitsCharacteristicsFiltered.add(comitCharac);
	        }
	      }
	    } else {
	      allCommitsCharacteristicsFiltered = new ArrayList<>(allCommitsCharacteristics);
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
	 * @return <code>true</code> if the commit should be removed, false otherwise
	 */
	private static boolean shouldFilter(CommitCharacteristics commitCharac, String textFilter) {
	  boolean shouldFilter = false;
	  if( textFilter != null &&  !textFilter.isEmpty()) {
	    String date = "";
	    String author = ""; 
	    String message = "";
	    String longCommitId = "";
      String shortCommitId = "";

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
        longCommitId = commitIdTemp.toLowerCase();
        if(longCommitId.length() >= SHORT_COMMIT_ID_LENGTH) {
          shortCommitId = longCommitId.substring(0,SHORT_COMMIT_ID_LENGTH);
        }
      }
	    String messageTemp = commitCharac.getCommitMessage();
	    if(messageTemp != null) {
	      message = messageTemp.toLowerCase();
	    }
	    shouldFilter = shouldFilterCommit(textFilter, date, author, shortCommitId, longCommitId, message);
	  } 
	  return shouldFilter;
	}
	
	/**
	 * Tells if a commit should be removed or not.
	 * 
	 * @param textFilter The text from filter field
	 * @param date The date of the commit
	 * @param author The author of the commit
	 * @param shortCommitId The short commit id
	 * @param longCommitId The full commit id 
	 * @param message The message of the commit
	 * 
	 * @return <code>true</code> if commit should be filtered
	 */
	private static boolean shouldFilterCommit(
      String textFilter,
      String date,
      String author,
      String shortCommitId,
      String longCommitId,
      String message) {
	  boolean shouldFilter = false;
	  String[] tokens = textFilter.split("[, .!-]+");
	  for (int i = 0; i < tokens.length; i++) {
	    String token = tokens[i].trim();
	    String lowercaseToken = token.toLowerCase();
	    if(!author.contains(lowercaseToken) &&
          !date.contains(token) &&
          !message.contains(lowercaseToken) &&
          !longCommitId.equals(lowercaseToken) &&
          !shortCommitId.equals(lowercaseToken)){
        return true;
      }
	  }
	  return shouldFilter;
	}

}
