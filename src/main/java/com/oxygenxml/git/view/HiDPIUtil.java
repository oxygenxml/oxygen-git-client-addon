/*
 * Copyright (c) 2018 Syncro Soft SRL - All Rights Reserved.
 *
 * This file contains proprietary and confidential source code.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */
package com.oxygenxml.git.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.html.HTMLEditorKit;

import ro.sync.svn.trees.treetable.SVNTreeTableColumnInfoPO;
import ro.sync.ui.hidpi.RetinaDetector;
import ro.sync.ui.table.TableColumnInfo;
import ro.sync.ui.table.TableColumnModelInfo;
import ro.sync.util.PlatformDetector;

/**
 * Class that holds utility methods used when HiDPI is detected.
 * 
 * @author alina_iordache
 */
public class HiDPIUtil {
  
  /**
   * Constructor.
   */
  private HiDPIUtil() {
    // Avoid instantiations
  }
  
  /**
   * Converts the given insets with the scaling factor if the HiDPI support is activated.
   * 
   * @param insets The current insets.
   * @return The current insets if the HiDPI support is not activated, the insets updated with 
   * scaling factor when support is activated.
   */
  public static Insets getHiDPIInsets(Insets insets) {
    return getHiDPIInsets(insets, RetinaDetector.getInstance().getScalingFactor());
  }
  
  /**
   * Converts the given insets with the scaling factor if the HiDPI support is activated.
   * 
   * @param insets The current insets.
   * @param scalingFactor The scaling factor.
   * @return The current insets if the HiDPI support is not activated, the insets updated with 
   * scaling factor when support is activated.
   */
  public static Insets getHiDPIInsets(Insets insets, float scalingFactor) {
    if (insets != null && 
        RetinaDetector.getInstance().isRetinaNoImplicitSupport()) {
      insets = new Insets(
          (int) (insets.top * scalingFactor), 
          (int) (insets.left * scalingFactor), 
          (int) (insets.bottom * scalingFactor), 
          (int) (insets.right * scalingFactor));
    }
    return insets;
  }

  /**
   * Builds a {@link Dimension} based on the given width and height,
   * but scaled with the scaling factor of the HiDPI support (if active).
   * 
   * <p>This method is preferred, instead of {@link #getHiDPIDimension(Dimension)},
   *    to avoid generating garbage <code>Dimension</code> objects just to generate new ones with the updated values.  
   *
   * @param width   The base width.
   * @param height  The base height.
   *
   * @return A <code>Dimension</code> object. Never <code>null</code>.
   * 
   * @see #getHiDPIDimension(Dimension)
   */
  public static Dimension getHiDPIDimension(int width, int height) {
    if (RetinaDetector.getInstance().isRetinaNoImplicitSupport()) {
      float scalingFactor = RetinaDetector.getInstance().getScalingFactor();
      width = (int) (width * scalingFactor);
      height = (int) (height * scalingFactor);
    }

    return new Dimension(width, height);
  }
  
  /**
   * Builds a new {@link Dimension} object based on a given one,
   * but scaled with the scaling factor of the HiDPI support (if active).
   * 
   * <p>If possible, it is preferable to use {@link #getHiDPIDimension(int, int)},
   *    instead of creating a new <code>Dimension</code> object right before calling this method,
   *    to avoid generating garbage in the memory.
   * 
   * @param dimension The base dimension. Can be <code>null</code>.
   *
   * @return The original dimension object, if it is <code>null</code> or if the HiDPI support is not active.
   *         Else, a new <code>Dimension</code> object.
   *
   * @see #getHiDPIDimension(int, int)
   */
  public static Dimension getHiDPIDimension(Dimension dimension) {
    return getHiDPIDimension(dimension, RetinaDetector.getInstance().getScalingFactor());
  }
  
  /**
   * Builds a new {@link Dimension} object based on a given one,
   * but scaled with the scaling factor of the HiDPI support (if active).
   * 
   * <p>If possible, it is preferable to use {@link #getHiDPIDimension(int, int)},
   *    instead of creating a new <code>Dimension</code> object right before calling this method,
   *    to avoid generating garbage in the memory.
   * 
   * @param dimension The base dimension. Can be <code>null</code>.
   * @param scalingFactor The scaling factor.
   *
   * @return The original dimension object, if it is <code>null</code> or if the HiDPI support is not active.
   *         Else, a new <code>Dimension</code> object.
   *
   * @see #getHiDPIDimension(int, int)
   */
  public static Dimension getHiDPIDimension(Dimension dimension, float scalingFactor) {
    if (dimension != null && 
        RetinaDetector.getInstance().isRetinaNoImplicitSupport()) {
      dimension = new Dimension(
          (int) (dimension.width * scalingFactor), 
          (int) (dimension.height * scalingFactor));
    }
    return dimension;
  }
  
  /**
   * Update insets, gaps and preferred sizes when HIDPI mode is detected.
   * 
   * @param component The component for which the insets, gaps and preferred sizes should be updated.
   * @param updatePreferredWidth <code>true</code> to update the preferred width.
   */
  public static void updateComponentsForHiDPI(Component component, boolean updatePreferredWidth) {
    updateComponentsForHiDPI(component, updatePreferredWidth, false);
  }
  
  /**
   * Update insets, gaps and preferred sizes when HIDPI mode is detected.
   * 
   * @param component The component for which the insets, gaps and preferred sizes should be updated.
   * @param updatePreferredWidth <code>true</code> to update the preferred width.
   * @param reset <code>true</code> to reset the insets, gaps and preferred sizes already updated.
   */
  public static void updateComponentsForHiDPI(Component component, boolean updatePreferredWidth, boolean reset) {
    if (component instanceof JPanel) {
      updatePanelForHiDPI((JPanel) component, updatePreferredWidth, true, reset);
    }

    if (component instanceof Container) {
      Component[] components = ((Container) component).getComponents();
      for (Component child : components) {
        updateComponentsForHiDPI(child, updatePreferredWidth, reset);
      }
    }
  }

  /**
   * Update panel components insets, gaps and preferred sizes when HIDPI mode is detected.
   * 
   * @param panel The panel for which the insets, gaps and preferred sizes should be updated.
   * @param updatePreferredWidth <code>true</code> to update the preferred width.
   * @param updatePreferredHeight <code>true</code> to update the preferred height.
   * @param reset <code>true</code> to reset the insets, gaps and preferred sizes already updated.
   */
  private static void updatePanelForHiDPI(JPanel panel, boolean updatePreferredWidth, boolean updatePreferredHeight, boolean reset) { 
    LayoutManager layout = panel.getLayout();
    float scalingFactor = RetinaDetector.getInstance().getScalingFactor();
    if (reset) {
      scalingFactor = 1 / scalingFactor;
    }
    updateGaps(layout, scalingFactor);
    Component[] components = panel.getComponents();
    for (Component child : components) {
      updateTableColumnsWidth(scalingFactor, child);
    }
    updatePreferredSize(components, updatePreferredWidth || !(layout instanceof GridBagLayout), updatePreferredHeight, scalingFactor);
    if (layout instanceof GridBagLayout) {
      // GridBag layout, update insets for child components
      GridBagLayout gridBagLayout = (GridBagLayout) layout;
      
      for (Component child : components) {
        GridBagConstraints constraints = 
            (GridBagConstraints) gridBagLayout.getConstraints(child).clone();
        
        Insets insets = constraints.insets;
        if (insets != null) {
          constraints.insets = getHiDPIInsets(insets, scalingFactor);
        }
        gridBagLayout.setConstraints(child, constraints);
      }
    }
  }

  /**
   * If the given component is a table, updates the columns width.
   * 
   * @param scalingFactor The scaling factor.
   * @param child The current component to be updated.
   */
  private static void updateTableColumnsWidth(float scalingFactor, Component child) {
    if (child instanceof JScrollPane) {
      JScrollPane scPane = (JScrollPane) child;
      Component view = scPane.getViewport().getView();
      if (view instanceof JTable) {
        JTable table = (JTable) view;
        Dimension preferredScrollableViewportSize = table.getPreferredScrollableViewportSize();
        table.setPreferredScrollableViewportSize(getHiDPIDimension(preferredScrollableViewportSize));
        TableColumnModel columnModel = table.getColumnModel();
        Enumeration<TableColumn> columns = columnModel.getColumns();
        while (columns.hasMoreElements()) {
          TableColumn tableColumn = columns.nextElement();
          int minWidth = tableColumn.getMinWidth();
          int width = tableColumn.getWidth();
          int preferredWidth = tableColumn.getPreferredWidth();
          int maxWidth = tableColumn.getMaxWidth();
          tableColumn.setMinWidth((int) (minWidth * scalingFactor));
          if (maxWidth != Integer.MAX_VALUE) {
            tableColumn.setMaxWidth((int) (maxWidth * scalingFactor));
          }
          tableColumn.setWidth((int) (width * scalingFactor));
          tableColumn.setPreferredWidth((int) (preferredWidth * scalingFactor));
        }
      }
    }
  }
  
  /**
   * Check if the given component is an editor pane with HTML editor kit.
   * 
   * @param component The component to check.
   * 
   * @return <code>true</code> if the given component is an editor pane with HTML editor kit.
   */
  private static boolean editorPaneWithHtmlContent(Component component) {
    return component instanceof JEditorPane && 
        ((JEditorPane) component).getEditorKit() instanceof HTMLEditorKit;
  }
  
  /**
   * Update preferred size.
   * 
   * @param components The components that should be iterated in order to update the preferred size.
   * @param updatePreferredWidth <code>true</code> to update the preferred width. 
   * @param updatePreferredHeight <code>true</code> to update the preferred height.
   * @param scalingFactor The scaling factor.
   */
  private static void updatePreferredSize(Component[] components, boolean updatePreferredWidth, boolean updatePreferredHeight, float scalingFactor) {
    for (Component child : components) {
      // Skip buttons and components without preferred size set.
      if (!(child instanceof AbstractButton) 
          && !editorPaneWithHtmlContent(child) 
          && child.isPreferredSizeSet()) {
        Dimension preferredSize = child.getPreferredSize();
        // Compute new preferred size only if the actual preferred size is not 0, 0
        if (preferredSize.width !=0 || preferredSize.height != 0) {
          if (child instanceof JComboBox || child instanceof JTextField 
              || child instanceof JTextArea || child instanceof JSpinner) {
            if (updatePreferredWidth) {
              // For text fields and combo boxes, only the width should be updated
              child.setPreferredSize(new Dimension(
                  (int) (preferredSize.width * scalingFactor),
                  preferredSize.height));
            }
          } else {
            if (updatePreferredWidth) {
              boolean update = true;
              if (child instanceof JPanel) {
                LayoutManager layout = ((JPanel) child).getLayout();
                update = !(layout instanceof CardLayout);
              }
              if (update) {
                if (updatePreferredHeight) {
                  // Update preferred width & height
                  child.setPreferredSize(getHiDPIDimension(preferredSize, scalingFactor));
                } else {
                  // Update only width
                  child.setPreferredSize(new Dimension((int) (preferredSize.width * scalingFactor), preferredSize.height));
                }
              }
            } else {
              // Update only preferred height
              if (updatePreferredHeight) {
                child.setPreferredSize(new Dimension(
                    preferredSize.width,
                    (int) (preferredSize.height * scalingFactor)));
              }
            }
          }
        }
      }
    }
  }
  
  /**
   * Update gaps between components.
   * 
   * @param layout        The layout manager.
   * @param scalingFactor The scaling factor.
   */
  private static void updateGaps(LayoutManager layout, float scalingFactor) {
    if (layout instanceof FlowLayout) {
      FlowLayout flowLayout = (FlowLayout) layout;
      flowLayout.setHgap((int) (flowLayout.getHgap() * scalingFactor));
      flowLayout.setVgap((int) (flowLayout.getVgap() * scalingFactor));
    } else if (layout instanceof BorderLayout) {
      BorderLayout borderLayout = (BorderLayout) layout;
      borderLayout.setHgap((int) (borderLayout.getHgap() * scalingFactor));
      borderLayout.setVgap((int) (borderLayout.getVgap() * scalingFactor));
    } else if (layout instanceof GridLayout) {
      GridLayout gridLayout = (GridLayout) layout;
      gridLayout.setHgap((int) (gridLayout.getHgap() * scalingFactor));
      gridLayout.setVgap((int) (gridLayout.getVgap() * scalingFactor));
    } else if (layout instanceof CardLayout) {
      CardLayout cardLayout = (CardLayout) layout;
      cardLayout.setHgap((int) (cardLayout.getHgap() * scalingFactor));
      cardLayout.setVgap((int) (cardLayout.getVgap() * scalingFactor));
    }
  }
  
  /**
   * Scale columns width.
   * 
   * @param tableColumnModel The table columns model.
   * @param onSave <code>true</code> if scaling is invoked when column model is saved.
   * @return The column model with scaled columns width.
   */
  public static TableColumnModelInfo<? extends TableColumnInfo> scaleColumnsWidth(
      TableColumnModelInfo<? extends TableColumnInfo> tableColumnModel, boolean onSave) {
    TableColumnModelInfo<? extends TableColumnInfo> scaledTableColumnsModel = 
        tableColumnModel != null ? (TableColumnModelInfo<? extends TableColumnInfo>) tableColumnModel.clone() : null;
    if (scaledTableColumnsModel != null && 
        RetinaDetector.getInstance().isRetinaNoImplicitSupport()) {
      float scalingFactor = RetinaDetector.getInstance().getScalingFactor();
      if (onSave) {
        scalingFactor = 1 / scalingFactor;
      }
      List<? extends TableColumnInfo> columns = scaledTableColumnsModel.getColumnsInfo();
      for (TableColumnInfo column : columns) {
        column.setMaxWidth((int) (column.getMaxWidth() * scalingFactor));
        column.setMinWidth((int) (column.getMinWidth() * scalingFactor));
        column.setWidth((int) (column.getWidth() * scalingFactor));
      }
    }
    return scaledTableColumnsModel;
  }
  
  /**
   * Scale SVN columns width.
   * 
   * @param svnColumnInfoPOs The array of the SVN tree table columns.
   * @param onSave <code>true</code> if scaling is invoked when column model is saved.
   * @return The array of the SVN tree table columns scaled.
   */
  public static SVNTreeTableColumnInfoPO[] scaleSVNColumnsWidth(SVNTreeTableColumnInfoPO[] svnColumnInfoPOs, boolean onSave) {
    if (RetinaDetector.getInstance().isRetinaNoImplicitSupport()) {
      SVNTreeTableColumnInfoPO[] newSvnColumnInfoPOs = new SVNTreeTableColumnInfoPO[svnColumnInfoPOs.length];
      
      float scalingFactor = RetinaDetector.getInstance().getScalingFactor();
      if (onSave) {
        scalingFactor = 1 / scalingFactor;
      }
      
      for (int i = 0; i < svnColumnInfoPOs.length; i++) {
        newSvnColumnInfoPOs[i] = svnColumnInfoPOs[i].clone();
        int minimumWidth = svnColumnInfoPOs[i].getMinimumWidth();
        int preferredWidth = svnColumnInfoPOs[i].getPreferredWidth();
        newSvnColumnInfoPOs[i].setMinimumWidth((int) (minimumWidth * scalingFactor));
        newSvnColumnInfoPOs[i].setPreferredWidth((int) (preferredWidth * scalingFactor));
      }
      
      return newSvnColumnInfoPOs;
    } else {
      return svnColumnInfoPOs;
    }
  }
  
  /**
   * Compute the dialog dimension for a HiDPI screen with no implicit support from the OS.
   * 
   * @param initialDimension  The initial dimension.
   * @param preferredDimension The dimension preferred by the component.
   * 
   * @return the computed dimension.
   */
  public static Dimension computeHiDPIDimensionForNoImplicitSupport(Dimension initialDimension, Dimension preferredSize) {
    Dimension newDim = initialDimension;
    float scalingFactor = RetinaDetector.getInstance().getScalingFactor();
    if (initialDimension.width < preferredSize.width) {
      if (PlatformDetector.isWin32()) {
        newDim.width *= scalingFactor;
        if (newDim.width < preferredSize.width) {
          newDim.width = preferredSize.width;
        }
      } else {
        // Use the preferred width for Linux.
        newDim.width = preferredSize.width;
      }
    }
    if (initialDimension.height < preferredSize.height) {
      if (PlatformDetector.isWin32()) {
        newDim.height *= scalingFactor;
        if (newDim.height < preferredSize.height) {
          newDim.height = preferredSize.height;
        }
      } else {
        // Use the preferred height for Linux.
        newDim.height = preferredSize.height;
      }
    }
    
    return newDim;
  }
}