package com.oxygenxml.git.view.history;

import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

/**
 * Table Model for Commit History Characteristics.
 * 
 * @Alexandra_Dinisor
 *
 */
public class HistoryCommitTableModel extends AbstractTableModel {

	/*
	 * Constants for the index representing the table column.
	 */
	public static final int COMMIT_MESSAGE = 0;
	public static final int DATE = 1;
	public static final int AUTHOR = 2;
	public static final int COMMIT_ABBREVIATED_ID = 3;

	/**
	 * The internal representation of the model.
	 */
	private List<CommitCharacteristics> allCommitsCharacteristics;

	/**
	 * Construct the Table Model with a Vector containing all commitCharacteristics.
	 * 
	 * @param commitVector The computed commitVector
	 */
	public HistoryCommitTableModel(List<CommitCharacteristics> commitVector) {
		this.allCommitsCharacteristics = commitVector;
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		CommitCharacteristics commitCharacteristics = allCommitsCharacteristics.get(rowIndex);
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
		return allCommitsCharacteristics != null ? allCommitsCharacteristics.size() : 0;
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
    return allCommitsCharacteristics;
  }

}
