package com.oxygenxml.git.view.staging;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.util.function.BooleanSupplier;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.view.util.RendererUtil;
import com.oxygenxml.git.view.util.RenderingInfo;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Renderer for the staged/unstaged tables.
 */
@SuppressWarnings("java:S110")
public final class StagingResourcesTableCellRenderer extends DefaultTableCellRenderer {
  
  /**
   * Tells if a contextual menu is presented over the table.
   */
  private BooleanSupplier contextMenuShowing;
  
  /**
   * The file/folder to show history.
   */
  private String searchedFilePath;


/**
   * The border for padding.
   */
  private static final Border PADDING = BorderFactory.createEmptyBorder(
      0, 
      UIConstants.COMPONENT_LEFT_PADDING - 1, 
      0, 
      UIConstants.COMPONENT_RIGHT_PADDING - 1
  );
  
  
  /**
   * Constructor.
   * 
   * @param contextMenuShowing Tells if a contextual menu is presented over the table.
   */
  public StagingResourcesTableCellRenderer(BooleanSupplier contextMenuShowing) {
    this.contextMenuShowing = contextMenuShowing;
  }
  
  
  /**
   * @see javax.swing.table.TableCellRenderer.getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)
   */
  @Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
	    int row, int column) {
    Icon icon = null;
    String tooltipText = null;
    String labelText = "";
    
    JLabel tableCellRendererComponent = (JLabel) super.getTableCellRendererComponent(
        table, value, isSelected, hasFocus, row, column);
    
    String location = "";
    if (value instanceof GitChangeType) {
      RenderingInfo renderingInfo = RendererUtil.getChangeRenderingInfo((GitChangeType) value);
      setBorder(BorderFactory.createCompoundBorder(getBorder(), PADDING));
      if (renderingInfo != null) {
        icon = renderingInfo.getIcon();
        tooltipText = renderingInfo.getTooltip();
      }
    } else if (value instanceof FileStatus) {
      location = ((FileStatus) value).getFileLocation();
      setBorder(BorderFactory.createCompoundBorder(getBorder(), PADDING));
      
      FontMetrics metrics = getFontMetrics(getFont());
      labelText = FileUtil.truncateText(location, metrics, table.getWidth() - table.getColumnModel().getColumn(0).getWidth());
      
      String description = ((FileStatus) value).getDescription();
      if (description != null) {
        tooltipText = description;
      } else {
        tooltipText = location;
        String fileName = tooltipText.substring(tooltipText.lastIndexOf('/') + 1);
        if (!fileName.equals(tooltipText)) {
          tooltipText = tooltipText.replace("/" + fileName, "");
          tooltipText = fileName + " - " + tooltipText;
        }
      } 
    }
    
    if(isSelected) {
      tableCellRendererComponent.setForeground(table.getSelectionForeground());
    } else {
      updateForegroundText(location, tableCellRendererComponent); 
    }
    
    tableCellRendererComponent.setIcon(icon);
    tableCellRendererComponent.setToolTipText(tooltipText);
    tableCellRendererComponent.setText(labelText);
    
    
    
    // Active/inactive table selection
    if (table.isRowSelected(row)) {
      if (table.hasFocus()) {
        tableCellRendererComponent.setBackground(table.getSelectionBackground());
      } else if (!contextMenuShowing.getAsBoolean()) {
        Color defaultColor = table.getSelectionBackground();
        tableCellRendererComponent.setBackground(RendererUtil.getInactiveSelectionColor(table, defaultColor));
      }
    } else {
      tableCellRendererComponent.setBackground(table.getBackground());
    }
    

    return tableCellRendererComponent;
  }


  /**
   * Set the value for the searched file.
   * 
   * @param selectedFilePath The new searched file.
   */
  public void setSearchedFilePath(String selectedFilePath) {
    this.searchedFilePath = selectedFilePath;
  }
  
  
  /**
   * Updates the text foreground.
   *  
   * @param currentFilePath                The current file path.
   * @param tableCellRendererComponent     The displayed Jlabel. 
   */
  private void updateForegroundText(String currentFilePath, JLabel tableCellRendererComponent) {
	 if (PluginWorkspaceProvider.getPluginWorkspace().getColorTheme() != null) {
      if(searchedFilePath instanceof String && 
          !(searchedFilePath.equals(currentFilePath) || currentFilePath.startsWith(searchedFilePath + "/", 0))) {
        tableCellRendererComponent.setForeground(
            PluginWorkspaceProvider.getPluginWorkspace().getColorTheme().isDarkTheme() ?
                UIUtil.NOT_SEARCHED_FILES_COLOR_GRAPHITE_THEME : UIUtil.NOT_SEARCHED_FILES_COLOR_LIGHT_THEME);
      } else {
        tableCellRendererComponent.setForeground(
            PluginWorkspaceProvider.getPluginWorkspace().getColorTheme().isDarkTheme() ?
                UIUtil.SEARCHED_FILES_COLOR_GRAPHITE_THEME : UIUtil.SEARCHED_FILES_COLOR_LIGHT_THEME);
      }
    }
  }
  
}