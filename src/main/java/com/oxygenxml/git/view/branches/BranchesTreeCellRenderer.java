package com.oxygenxml.git.view.branches;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.utils.TreeUtil;
import com.oxygenxml.git.view.historycomponents.RoundedLineBorder;
import com.oxygenxml.git.view.renderer.RendererUtil;
import com.oxygenxml.git.view.renderer.RenderingInfo;

/**
 * Renderer for the nodes icon in the branches tree, based on the path to the
 * node.
 * 
 * @author Bogdan Draghici
 *
 */
public class BranchesTreeCellRenderer extends DefaultTreeCellRenderer {
  /**
   * Default selection color.
   */
  private final Color defaultSelectionColor = getBackgroundSelectionColor();
  /**
   * Tells us if the context menu is showing.
   */
  private BooleanSupplier isContextMenuShowing;
  /**
   * Supplies the current branch.
   */
  private Supplier<String> currentBranchNameSupplier;
  
  /**
   * Constructor.
   * 
   * @param isContextMenuShowing Tells us if the context menu is showing.
   * @param currentBranchNameSupplier Gives us the current branch name.
   */
  public BranchesTreeCellRenderer(
      BooleanSupplier isContextMenuShowing,
      Supplier<String> currentBranchNameSupplier) {
    this.isContextMenuShowing = isContextMenuShowing;
    this.currentBranchNameSupplier = currentBranchNameSupplier;
  }

  /**
   * @see DefaultTreeCellRenderer.getTreeCellRendererComponent(JTree, Object, boolean, boolean, boolean, int, boolean)
   */
  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
      int row, boolean hasFocus) {

    JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    
    Icon icon = Icons.getIcon(Icons.LOCAL_REPO);
    String path = "";
    TreePath treePath = tree.getPathForRow(row);
    if (treePath != null) {
      path = TreeUtil.getStringPath(treePath);
      if (!path.isEmpty()) {
        RenderingInfo renderingInfo = RendererUtil.getRenderingInfo(path);
        if (renderingInfo != null) {
          icon = renderingInfo.getIcon();
        }
      }
    }

    if (label != null) {
      RoundedLineBorder roundedLineBorder = new RoundedLineBorder(label.getForeground(), 1, 8, true);
      EmptyBorder emptyBorder = new EmptyBorder(roundedLineBorder.getBorderInsets(this));
      Font font = label.getFont();
      label.setIcon(icon);
      label.setIconTextGap(18);
      label.setHorizontalAlignment(SwingConstants.CENTER);
      label.setFont(font.deriveFont(Font.PLAIN));
      label.setBorder(emptyBorder);
      if (!path.isEmpty()) {
        String[] split = path.split("/");
        if (split[split.length - 1].equals(currentBranchNameSupplier.get())) {
          label.setFont(font.deriveFont(Font.BOLD));
          label.setBorder(roundedLineBorder);
        }
      }
      
      // Active/inactive table selection
      if (sel) {
        if (tree.hasFocus()) {
          setBorderSelectionColor(new Color(102,167,232));
          setBackgroundSelectionColor(defaultSelectionColor);
        } else if (!isContextMenuShowing.getAsBoolean()) {
          // TODO: why does the selection sometimes become inactive when showing the context menu?????????????? 
          // Do not render the tree as inactive if we have a contextual menu over it.
          setBackgroundSelectionColor(RendererUtil.getInactiveSelectionColor(tree, defaultSelectionColor));
        }
      }
    }

    return label;
  }
  
  /**
   * Paints the node, and in case it is also selected, take care not to draw the dashed rectangle border.
   * @see com.oxygenxml.git.view.branches.BranchesTreeCellRenderer.paint(Graphics)
   */
  @Override
  public void paint(Graphics g) {
    if (selected) {
      hasFocus = false;
      super.paint(g);
      paintBorder(g, 0, 0, getWidth(), getHeight());      
    }else {
      super.paint(g);
    }
  }
  /**
   * Paints the border for the selected node.
   * @param g
   * @param x
   * @param y
   * @param width
   * @param height
   */
  private void paintBorder(Graphics g, int x, int y, int w, int h) {
    Color bsColor = getBorderSelectionColor();

    if (bsColor != null && (selected)) {
        g.setColor(bsColor);
        g.drawRect(x, y, w - 1, h - 1);
    }
  }
}
