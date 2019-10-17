package com.oxygenxml.git.view.renderer;

import java.awt.Color;
import java.awt.Component;
import java.util.function.Supplier;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.renderer.RenderingInfo;

/**
 * Renderer for the staged/unstaged tables.
 */
public final class StagingResourcesTableCellRenderer extends DefaultTableCellRenderer {
  /**
   * Tells if a contextual menu is presented over the table.
   */
  private Supplier<Boolean> contextMenuShowing;
  /**
   * Constructor.
   * 
   * @param contextMenuShowing Tells if a contextual menu is presented over the table.
   */
  public StagingResourcesTableCellRenderer(Supplier<Boolean> contextMenuShowing) {
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
    
    if (value instanceof GitChangeType) {
      RenderingInfo renderingInfo = RendererUtil.getRenderingInfo((GitChangeType) value);
      if (renderingInfo != null) {
        icon = renderingInfo.getIcon();
        tooltipText = renderingInfo.getTooltip();
      }
    } else if (value instanceof String) {
      tooltipText = (String) value;
      String fileName = tooltipText.substring(tooltipText.lastIndexOf('/') + 1);
      if (!fileName.equals(tooltipText)) {
        tooltipText = tooltipText.replace("/" + fileName, "");
        tooltipText = fileName + " - " + tooltipText;
      }
      labelText = (String) value;
    }
    
    tableCellRendererComponent.setIcon(icon);
    tableCellRendererComponent.setToolTipText(tooltipText);
    tableCellRendererComponent.setText(labelText);
    
    // Active/inactive table selection
    if (table.isRowSelected(row)) {
      if (table.hasFocus()) {
        tableCellRendererComponent.setBackground(table.getSelectionBackground());
      } else if (!contextMenuShowing.get()) {
        Color defaultColor = table.getSelectionBackground();
        tableCellRendererComponent.setBackground(RendererUtil.getInactiveSelectionColor(table, defaultColor));
      }
    } else {
      tableCellRendererComponent.setBackground(table.getBackground());
    }

    return tableCellRendererComponent;
  }
}