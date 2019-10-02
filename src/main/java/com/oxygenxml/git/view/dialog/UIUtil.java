package com.oxygenxml.git.view.dialog;

import java.util.function.Supplier;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.table.TableColumn;

import org.apache.log4j.Logger;

import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.utils.UndoSupportInstaller;
import com.oxygenxml.git.view.StagingResourcesTableModel;
import com.oxygenxml.git.view.renderer.StagingResourcesTableCellRenderer;

/**
 * Utility class for UI-related issues. 
 */
public class UIUtil {
  
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(UIUtil.class.getName());
  
  /**
   * Hidden constructor.
   */
  private UIUtil() {
    // Nothing
  }
  
  /**
   * This method first tries to create an undoable text field,
   * that also has a contextual menu with editing action. If this is not possible,
   * a standard Java text field is created.
   * 
   * @return the text field.
   */
  public static JTextField createTextField() {
    JTextField textField = null;
    try {
      Class<?> textFieldClass= Class.forName("ro.sync.exml.workspace.api.standalone.ui.TextField");
      textField = (JTextField) textFieldClass.newInstance();
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e1) {
      textField = new JTextField();
      UndoSupportInstaller.installUndoManager(textField);
      if (logger.isDebugEnabled()) {
        logger.debug(e1, e1);
      }
    }
    return textField;
  }
  
  
  /**
   * @return The tree that presents the resources. 
   */
  public static JTree createTree() {
    JTree t = null;
    try {
      Class<?> treeClass = UIUtil.class.getClassLoader().loadClass("ro.sync.exml.workspace.api.standalone.ui.Tree");
      t = (JTree) treeClass.newInstance();
    } catch (Exception e) {
      logger.debug(e, e);
    }
    
    if (t == null) {
      t = new JTree();
    }
    
    return t;
  }
  
  /**
   * @param fileTableModel The model for the table.
   * 
   * @return The table that presents the resources.
   */
  public static JTable createTable() {
    JTable table = null;
    try {
      Class<?> tableClass = UIUtil.class.getClassLoader().loadClass("ro.sync.exml.workspace.api.standalone.ui.Table");
      table = (JTable) tableClass.newInstance();
    } catch (Exception e) {
      logger.debug(e, e);
    }
    
    if (table == null) {
      table = new JTable();
    }
    
    return table;
  }
  
  /**
   * @param fileTableModel The model for the table.
   * 
   * @return The table that presents the resources.
   */
  public static JTable createResourcesTable(
      StagingResourcesTableModel fileTableModel, 
      Supplier<Boolean> contextMenuShowing) {
    JTable table = UIUtil.createTable();
    table.setModel(fileTableModel);
    
    table.setTableHeader(null);
    table.setShowGrid(false);
    
    
    Icon icon = ImageConstants.getIcon(ImageConstants.GIT_ADD_ICON);
    int iconWidth = icon.getIconWidth();
    TableColumn statusCol = table.getColumnModel().getColumn(StagingResourcesTableModel.FILE_STATUS_COLUMN);
    statusCol.setPreferredWidth(iconWidth);
    statusCol.setMaxWidth(iconWidth + 4);

    table.setDefaultRenderer(Object.class, new StagingResourcesTableCellRenderer(contextMenuShowing));
    
    
    return table;
  }

}
