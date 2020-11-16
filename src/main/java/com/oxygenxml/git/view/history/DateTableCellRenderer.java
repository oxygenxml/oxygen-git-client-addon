package com.oxygenxml.git.view.history;

import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renders a date according to a format.
 */
public class DateTableCellRenderer extends DefaultTableCellRenderer {
  /**
   * Date format.
   */
  private SimpleDateFormat format;

  /**
   * Constructor.
   * 
   * @param format Date format.
   */
  public DateTableCellRenderer(String format) {
    this.format = new SimpleDateFormat(format);
  }
  
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
      int row, int column) {
    if (value instanceof Date) {
      Date date = (Date) value;
      value = format.format(date);
    }
    
    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
  }

}
