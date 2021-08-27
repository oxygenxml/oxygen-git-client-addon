package com.oxygenxml.git.view.util;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;

import org.apache.log4j.Logger;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.staging.StagingResourcesTableCellRenderer;
import com.oxygenxml.git.view.staging.StagingResourcesTableModel;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.WSEditorPage;
import ro.sync.exml.workspace.api.editor.page.author.WSAuthorEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ViewInfo;
import ro.sync.exml.workspace.api.standalone.ui.Table;
import ro.sync.exml.workspace.api.util.ColorTheme;

/**
 * Utility class for UI-related issues. 
 */
public class UIUtil {
  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(UIUtil.class);
  /**
   * Meta symbol.
   */
  private static final char MAC_META_SYMBOL = (char) 0x2318;
  /**
   * Shift symbol.
   */
  private static final char SHIFT_SYMBOL = (char) 0x21E7;
  /**
   * Alt symbol.
   */
  private static final char ALT_SYMBOL = (char) 0x2325;
  /**
   * Ctrl symbol.
   */
  private static final char CTRL_SYMBOL = (char) 0x2303;
  /**
   * Extra width for the icon column of the resources table (for beautifying reasons).
   */
  private static final int RESOURCE_TABLE_ICON_COLUMN_EXTRA_WIDTH = 3;
  /**
   * Dummy minimum width.
   */
  public static final int DUMMY_MIN_WIDTH = 1;
  
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
    JTable table = new Table() {
      @Override
      public JToolTip createToolTip() {
        return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
      }
    };
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
    JTextArea msgArea = new JTextArea(text) { // NOSONAR
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
    msgArea.setFocusable(false);
    msgArea.setOpaque(false);
    msgArea.setBorder(BorderFactory.createEmptyBorder());

    Font font = UIManager.getFont("Label.font");
    if (font != null) {
      msgArea.setFont(font);
    }
    
    return msgArea;
  }
  
  /**
   * Draws a hint text inside a text component.
   *
   * @param component Text component.
   * @param g Graphics used to draw.
   * @param text The text to draw.
   * @param hintColor An explicit color for the hint. <code>null</code> to use system default color.
   */
  public static void drawHint(JComponent component, Graphics g, String text, Color hintColor) {
    FontMetrics fm = component.getFontMetrics(g.getFont());
    int x = 0;
    int y = 0;
    int availableHeight = component.getHeight();

    // Adjust for insets
    Insets insets = component.getInsets();
    if (insets != null) {
      x += insets.left;
      y += insets.top;
      availableHeight -= insets.top;
      availableHeight -= insets.bottom;
    }

    // Adjust the y if the font height is different than the available height
    // @see BasicTextFieldUI#getBaseline(javax.swing.JComponent, int, int)
    int fontHeight = fm.getHeight();
    if (availableHeight != fontHeight) {
      y += (availableHeight - fontHeight) / 2;
    }

    y += fm.getAscent();

    if (hintColor != null) {
      g.setColor(hintColor);
    } else {
      g.setColor(new Color(128, 128, 128));
    }
    
    
    if (g instanceof Graphics2D) {
      final Graphics2D g2d = (Graphics2D) g;
      Object originalAntialiasingHint = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
      // update antialiasing
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      try {
        g2d.drawString(text, x, y);
      } finally {
        // Restore original settings
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalAntialiasingHint);
      }
    } else {
      g.drawString(text, x, y);
    }
  }
  
  /**
   *  Gets a string containing the UNICODE codes of the key modifier(s).
   * 
   * @param modifiers   The specified key modifiers
   * @param independent <code>true</code> to use a platform independent representation using 'M' keys. 
   * 
   * @return The string representation of the modifier(s)
   */
  public static String getKeyModifiersSymbol(int modifiers, boolean independent) {
    StringBuilder result = new StringBuilder();
    if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
      if (independent) {
        result.append("M4 ");
      } else {
        result.append(CTRL_SYMBOL + " ");
      }
    }
    if ((modifiers & KeyEvent.ALT_DOWN_MASK) != 0) {
      if (independent) {
        result.append("M3 ");
      } else {
        result.append(ALT_SYMBOL + " ");
      }
    }
    if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
      if (independent) {
        result.append("M2 ");
      } else {
        result.append(SHIFT_SYMBOL + " ");
      }
    }
    if ((modifiers & KeyEvent.META_DOWN_MASK) != 0) {
      if (independent) {
        result.append("M1 ");
      } else {
        result.append(MAC_META_SYMBOL + " ");
      }
    }
    return result.toString();
  }

  /**
   * Set border to the given component.
   * 
   * @param scrollPane The scroll pane.
   */
  public static void setDefaultScrollPaneBorder(JScrollPane scrollPane) {
    ColorTheme colorTheme = PluginWorkspaceProvider.getPluginWorkspace().getColorTheme();
    if (colorTheme != null) {
      // Can be null from tests
      scrollPane.setBorder(BorderFactory.createLineBorder(colorTheme.isDarkTheme() ? Color.GRAY : Color.LIGHT_GRAY));
    }
  }
  
  /**
   * @return The installMultilineTooltip method or <code>null</code> if it's unavailable in the current Oxygen.
   */
  public static Method getInstallMultilineTooltipMethod() {
    Method installMultilineTooltip = null;
    try {
      Class<?> uiCompsFactory = Class.forName(
          "ro.sync.exml.workspace.api.standalone.ui.OxygenUIComponentsFactory");
      installMultilineTooltip = uiCompsFactory.getMethod("installMultilineTooltip", JComponent.class);
    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
      logger.debug(e, e);
    }
    return installMultilineTooltip;
  }
  
  /**
   * Install a multiline tooltip on the component.
   * 
   * @param component Component on which to install the tooltip.
   * 
   * @return The installed tooltip, if one was installed.
   */
  public static Optional<JToolTip> createMultilineTooltip(JComponent component) {
    try {
      Method installMultilineTooltip = UIUtil.getInstallMultilineTooltipMethod();
      if (installMultilineTooltip != null) {
        return Optional.of((JToolTip) installMultilineTooltip.invoke(null, component));
      }
    } catch (SecurityException | IllegalAccessException | IllegalArgumentException 
        | InvocationTargetException e) {
      logger.debug(e, e);
    }
    
    return Optional.empty();
  }

}
