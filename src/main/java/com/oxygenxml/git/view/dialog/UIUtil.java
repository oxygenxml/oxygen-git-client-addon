package com.oxygenxml.git.view.dialog;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;
import java.util.List;
import java.util.function.BooleanSupplier;

import javax.accessibility.AccessibleContext;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;

import org.apache.log4j.Logger;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.UndoSupportInstaller;
import com.oxygenxml.git.view.StagingResourcesTableModel;
import com.oxygenxml.git.view.renderer.StagingResourcesTableCellRenderer;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.WSEditorPage;
import ro.sync.exml.workspace.api.editor.page.author.WSAuthorEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ViewInfo;

/**
 * Utility class for UI-related issues. 
 */
public class UIUtil {
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(UIUtil.class.getName());
  /**
   * Extra width for the icon column of the resources table (for beautifying reasons).
   */
  private static final int RESOURCE_TABLE_ICON_COLUMN_EXTRA_WIDTH = 3;
  
  /**
   * Hidden constructor.
   */
  private UIUtil() {
    // Nothing
  }
  
  /**
   * Set busy cursor or default.
   * 
   * @param isSetBusy <code>true</code> to set busy cursor.
   * @param views     A list of views on which to show a busy or default cursor.
   */
  public static void setBusyCursor(boolean isSetBusy, List<ViewInfo> views) {
    StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    if (isSetBusy) {
      SwingUtilities.invokeLater(() -> {
        Cursor busyCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        
        ((JFrame) pluginWS.getParentFrame()).setCursor(busyCursor);
        for (ViewInfo viewInfo : views) {
          viewInfo.getComponent().setCursor(busyCursor);
        }
        setEditorPageCursor(busyCursor);
      });
    } else {
      SwingUtilities.invokeLater(() -> {
        Cursor defaultCursor = Cursor.getDefaultCursor();
        
        ((JFrame) pluginWS.getParentFrame()).setCursor(defaultCursor);
        for (ViewInfo viewInfo : views) {
          viewInfo.getComponent().setCursor(defaultCursor);
        }
        setEditorPageCursor(defaultCursor);
      });
    }
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
   * Compute necessary height for text area with <b>line wrap and wrap style word</b>
   *  to display contained all rows.
   *
   * @param textComp The text area.
   * @param width The fixed width of the text area.
   * @param maxHeight The maximum allowed height.
   * @return The display height.
   */
  public static int computeHeight(JTextComponent textComp, int width, int maxHeight) {
    View view = textComp.getUI().getRootView(textComp);
    view.setSize(width, 0);
    int height = (int) view.getPreferredSpan(View.Y_AXIS);
   
    Insets insets = textComp.getInsets();
    if (insets != null) {
      height += insets.top + insets.bottom;      
    }
   
    if (maxHeight < height) {
      return maxHeight;
    }
    return height;
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
   * Creates a git resource table widget and install renderers on it.
   * 
   * @param fileTableModel The model for the table.
   * @param contextMenuShowing Can tell if a contextual menu is showing over the table.
   * 
   * @return The table that presents the resources.
   */
  public static JTable createResourcesTable(
      StagingResourcesTableModel fileTableModel, 
      BooleanSupplier contextMenuShowing) {
    JTable table = UIUtil.createTable();
    table.setModel(fileTableModel);
    
    table.getColumnModel().setColumnMargin(0);
    table.setTableHeader(null);
    table.setShowGrid(false);
    
    
    Icon icon = Icons.getIcon(Icons.GIT_ADD_ICON);
    int iconWidth = icon.getIconWidth();
    int colWidth = iconWidth + RESOURCE_TABLE_ICON_COLUMN_EXTRA_WIDTH;
    TableColumn statusCol = table.getColumnModel().getColumn(StagingResourcesTableModel.FILE_STATUS_COLUMN);
    statusCol.setMinWidth(colWidth);
    statusCol.setPreferredWidth(colWidth);
    statusCol.setMaxWidth(colWidth);

    table.setDefaultRenderer(Object.class, new StagingResourcesTableCellRenderer(contextMenuShowing));
    
    
    return table;
  }
  
  /**
   * Add actions at the bottom of the pop-up.
   * 
   * @param popUp   The pop-up.
   * @param actions The actions.
   */
  public static void addGitActions(JPopupMenu popUp, List<AbstractAction> actions) {
    popUp.addSeparator();
    JMenu gitMenu = new JMenu(Translator.getInstance().getTranslation(Tags.GIT));
    Icon icon = Icons.getIcon(Icons.GIT_ICON);
    gitMenu.setIcon(icon);
    for (AbstractAction action : actions) {
        gitMenu.add(action);
    }
    popUp.add(gitMenu);
  }
  
  /**
   * Set editor page cursor.
   * 
   * @param cursor   The cursor to set.
   */
  public static void setEditorPageCursor(Cursor cursor) {
    StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    WSEditor currentEditorAccess = pluginWS.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
    if (currentEditorAccess != null) {
      WSEditorPage currentPage = currentEditorAccess.getCurrentPage();
      JComponent pageComp = null;
      if (currentPage instanceof WSAuthorEditorPage) {
        pageComp = (JComponent) ((WSAuthorEditorPage) currentPage).getAuthorComponent();
      } else if (currentPage instanceof WSTextEditorPage) {
        pageComp = (JComponent) ((WSTextEditorPage) currentPage).getTextComponent();
      }
      if (pageComp != null) {
        pageComp.setCursor(cursor);
      }
    }
  }
  
  
  /**
   * Create a multiline message area.
   * 
   * @param text Text to present.
   * 
   * @return The message area.
   */
  public static JTextArea createMessageArea(String text) {
    JTextArea msgArea = new JTextArea(text) {
      /**
       * @see javax.swing.JTextArea#getAccessibleContext()
       */
      @Override
      public AccessibleContext getAccessibleContext() {
        return new JLabel(getText()).getAccessibleContext();
      }
    };
    
    msgArea.setWrapStyleWord(true);
    msgArea.setLineWrap(true);
    msgArea.setEditable(false);
    msgArea.setCaretPosition(0);
    msgArea.setHighlighter(null);
    // Reject focus in the message area.
    msgArea.setFocusable(false);

    Font font = UIManager.getFont("Label.font");
    if (font != null) {
      msgArea.setFont(font);
    }
    msgArea.setOpaque(false);
    
    Border emptyBorder = BorderFactory.createEmptyBorder();
    msgArea.setBorder(emptyBorder);
    
    return msgArea;
  }

}
