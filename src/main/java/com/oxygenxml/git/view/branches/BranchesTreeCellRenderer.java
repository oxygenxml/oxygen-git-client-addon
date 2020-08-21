package com.oxygenxml.git.view.branches;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.utils.TreeUtil;
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
   * @see javax.swing.tree.DefaultTreeCellRenderer.getTreeCellRendererComponent(JTree,
   *      Object, boolean, boolean, boolean, int, boolean)
   */
  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
      int row, boolean hasFocus) {

    JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    
    // TODO: mark the current branch

    Icon icon = Icons.getIcon(Icons.LOCAL_REPO);

    TreePath treePath = tree.getPathForRow(row);
    if (treePath != null) {
      String path = TreeUtil.getStringPath(treePath);
      if (!path.isEmpty()) {
        RenderingInfo renderingInfo = RendererUtil.getRenderingInfo(path);
        if (renderingInfo != null) {
          icon = renderingInfo.getIcon();
        }
      }
    }

    if (label != null) {
      label.setIcon(icon);

      // Active/inactive table selection
      if (sel) {
        if (tree.hasFocus()) {
          setBackgroundSelectionColor(defaultSelectionColor);
        } else {
          // Do not render the tree as inactive if we have a contextual menu over it.
          setBackgroundSelectionColor(RendererUtil.getInactiveSelectionColor(tree, defaultSelectionColor));
        }
      }
    }

    return label;
  }
}
