package com.oxygenxml.git.view.tags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class TagsTableModel extends AbstractTableModel {
  
  /**
   * The list of Tags.
   */
  private List<GitTag> gitTags = new ArrayList<>();
  
  /**
   * The list of Column Names
   */
  private List<String> columnNames;
  
  /**
   * <code>true</code> if the table is editable.
   */
  private boolean isTableEditable = false;

  /**
   * Constructor 
   * 
   * @param columnNames the names of the Table Columns
   */
  public TagsTableModel(String[] columnNames) {
    this.columnNames = Arrays.asList(columnNames);
  }
  
  /**
   * Set the list of Git Tags
   * 
   * @param gitTags a list of tags
   */
  public void setGitTags(List<GitTag> gitTags) {
    this.gitTags.clear();
    this.gitTags.addAll(gitTags);
    fireTableDataChanged();
  }
  
  /**
   * Get index of a tag in model.
   *
   * @param tag The tag.
   *
   * @return The index or <code>-1</code>.
   */
  public int getIndexOf(Object tag) {
    return gitTags.indexOf(tag);
  }
  
  /**
   * Remove a tag. 
   * 
   * @param tag Tag to remove.
   */
  public void remove(Object tag) {
    int index = getIndexOf(tag);
    if (index != -1) {
      gitTags.remove(index);
      fireTableRowsDeleted(index, index);
    }
  }
  
  /**
   * Add a tag. 
   * 
   * @param tag Tag to add.
   */
  public void add(GitTag tag) {
    gitTags.add(tag);
    fireTableRowsInserted(gitTags.size() - 1, gitTags.size() - 1);
  }
  
  /**
   * Get an GitTag at row index.
   * 
   * @param rowIndex The row index.
   *
   * @return The GitTag.
   */
  public GitTag getItemAt(int rowIndex) {
    return gitTags.get(rowIndex);
  }  
  
  /**
   * Get the row count
   * 
   * @return row count
   */
  @Override
  public int getRowCount() {
    return gitTags.size();
  }

  /**
   * Get the column count
   * 
   * @return column count
   */
  @Override
  public int getColumnCount() {
    return 2;
  }

  /**
   * Get value at specified position
   *
   * @param rowIndex The row index.
   * @param columnIndex The column index.
   * 
   * @return the value at specified position
   */
  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    switch (columnIndex) {
    case 0:
      return gitTags.get(rowIndex).getName();
    case 1:
      return gitTags.get(rowIndex).getMessage();
    default:
      return null;
    }
  }
  
  /**
   * Get the name of the column
   * 
   * @param column The column 
   * 
   * @return The name of the column
   */
  @Override
  public String getColumnName(int column) {
    return columnNames.get(column);
  }
  
  /**
   * @param rowIndex  Row index.
   * @param colIndex  Column index.
   *
   * @return <code>true</code> if editable.
   */
  @Override
  public boolean isCellEditable(int rowIndex, int colIndex) {      
    return isTableEditable;
  }
  
  /**
   * @return The inner list of tags.
   */
  public List<GitTag> getAttrsItems() {
    return gitTags;
  }
  
}
