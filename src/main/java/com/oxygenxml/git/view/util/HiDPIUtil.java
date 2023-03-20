/*
 * Copyright (c) 2018 Syncro Soft SRL - All Rights Reserved.
 *
 * This file contains proprietary and confidential source code.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */
package com.oxygenxml.git.view.util;

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

import com.oxygenxml.git.utils.PlatformDetectionUtil;

import ro.sync.ui.hidpi.RetinaDetector;

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
   * Applies a scaling factor depending if we are on a hidpi display.
   * 
   * @param width Width to scale.
   * 
   * @return A scaled width.
   */
  public static int scaleWidth(int width) {
    float scalingFactor = (float) 1.0;
    if (HiDPIUtil.isRetinaNoImplicitSupport()) {
      scalingFactor = HiDPIUtil.getScalingFactor();
    }

    return (int) (scalingFactor * width);
  }
  
  /**
   * Converts the given insets with the scaling factor if the HiDPI support is activated.
   * 
   * @param insets The current insets.
   * @return The current insets if the HiDPI support is not activated, the insets updated with 
   * scaling factor when support is activated.
   */
  public static Insets getHiDPIInsets(Insets insets) {
    return getHiDPIInsets(insets, getScalingFactor());
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
    if (insets != null && isRetinaNoImplicitSupport()) {
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
    if (isRetinaNoImplicitSupport()) {
      float scalingFactor = getScalingFactor();
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
    return getHiDPIDimension(dimension, getScalingFactor());
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
    if (dimension != null && isRetinaNoImplicitSupport()) {
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
    float scalingFactor = getScalingFactor();
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
        if (preferredSize.width != 0 || preferredSize.height != 0) {
          computePreferredSize(updatePreferredWidth, updatePreferredHeight, scalingFactor, child,
              preferredSize);
        }
      }
    }
  }

  /**
   * Compute the new preferred size for a child of the component.
   * 
   * @param updatePreferredWidth   <code>true</code> to update the preferred width. 
   * @param updatePreferredHeight  <code>true</code> to update the preferred height.
   * @param scalingFactor          The scaling factor.
   * @param child                  The component to compute preferred size.
   * @param preferredSize          The initial preferredSize.
   */
  private static void computePreferredSize(
      boolean updatePreferredWidth,
      boolean updatePreferredHeight,
      float scalingFactor,
      Component child,
      Dimension preferredSize) {
    if (shouldComputePrefferedSizeForComponent(child)) {
      if (updatePreferredWidth) {
        // For text fields and combo boxes, only the width should be updated
        child.setPreferredSize(new Dimension(
            (int) (preferredSize.width * scalingFactor),
            preferredSize.height));
      }
    } else if (updatePreferredWidth) {
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
    } else if (updatePreferredHeight) {
      child.setPreferredSize(new Dimension(
          preferredSize.width,
          (int) (preferredSize.height * scalingFactor)));
    }
  }

  /**
   * @param comp The component.
   * 
   * @return <code>true</code> if we should compute the preferred size for the given component.
   */
  private static boolean shouldComputePrefferedSizeForComponent(Component comp) {
    return comp instanceof JComboBox || comp instanceof JTextField 
        || comp instanceof JTextArea || comp instanceof JSpinner;
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
   * Compute the dialog dimension for a HiDPI screen with no implicit support from the OS.
   * 
   * @param initialDimension The initial dimension.
   * @param preferredSize    The dimension preferred by the component.
   * 
   * @return the computed dimension.
   */
  public static Dimension computeHiDPIDimensionForNoImplicitSupport(Dimension initialDimension, Dimension preferredSize) {
    Dimension newDim = initialDimension;
    float scalingFactor = getScalingFactor();

    boolean isWin = PlatformDetectionUtil.isWin();
    if (initialDimension.width < preferredSize.width) {
      if (isWin) {
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
      if (isWin) {
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
  
  /**
   * @return <code>true</code> if it is the Windows style Retina, where the application
   * must explicitly change its font and use double sized icons. 
   */
  public static boolean isRetinaNoImplicitSupport() {
    return RetinaDetector.getInstance().isRetinaNoImplicitSupport();
  }
  
  /**
   * @return Returns the HiDPI scaling factor, or 1 if there is no scaling. 
   * On Mac OS X with retina display this factor is 2.  
   */
  public static float getScalingFactor() {
    return RetinaDetector.getInstance().getIconScalingFactor();
  }
  
}