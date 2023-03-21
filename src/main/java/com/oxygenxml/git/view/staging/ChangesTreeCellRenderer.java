package com.oxygenxml.git.view.staging;

import java.awt.Color;
import java.awt.Component;
import java.util.function.BooleanSupplier;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.util.RendererUtil;
import com.oxygenxml.git.view.util.RenderingInfo;
import com.oxygenxml.git.view.util.TreeUtil;

/**
 * Renderer for the leafs icon in the tree, based on the git change type file status.
 * 
 * @author Beniamin Savu
 *
 */
public class ChangesTreeCellRenderer extends DefaultTreeCellRenderer {
  /**
   * Default selection color.
   */
  private final Color defaultSelectionColor = getBackgroundSelectionColor();
  /**
   * Tells if a contextual menu is active over the component.
   */
  private BooleanSupplier contextMenuShowing;
  
  /**
   * Constructor.
   * 
   * @param contextualMenuShowing Tells if a contextual menu is active over the component.
   */
  public ChangesTreeCellRenderer(BooleanSupplier contextualMenuShowing) {
    this.contextMenuShowing = contextualMenuShowing;
  }
  
  /**
   * @see javax.swing.tree.DefaultTreeCellRenderer.getTreeCellRendererComponent(JTree, Object, boolean, boolean, boolean, int, boolean)
   */
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		label = getUpdatedJLabel(tree, sel, row, label);
    return label;
	}

	/**
	 * Update the given label for the current node.
	 * 
	 * @param tree   The current tree.
	 * @param sel    <code>true</code> when selected.
	 * @param row    The current row.
	 * @param label  The super label.
	 * 
	 * @return The updated label.
	 */
  private JLabel getUpdatedJLabel(JTree tree, boolean sel, int row, JLabel label) {
    Icon icon = Icons.getIcon(Icons.FOLDER_TREE_ICON);
		String toolTip = null;

		TreePath treePath = tree.getPathForRow(row);
		if (treePath != null) {
			String path = TreeUtil.getStringPath(treePath);
			final StagingResourcesTreeModel model = (StagingResourcesTreeModel) tree.getModel();
			if (!"".equals(path) && model.isLeaf(TreeUtil.getTreeNodeFromString(model, path))) {
				FileStatus file = model.getFileByPath(path);
				if (file != null) {
				  GitChangeType changeType = file.getChangeType();
				  RenderingInfo renderingInfo = RendererUtil.getChangeRenderingInfo(changeType);
				  if (renderingInfo != null) {
				    icon = renderingInfo.getIcon();
				    toolTip = renderingInfo.getTooltip();
				  }
				  
				  if (file.getDescription() != null) {
				    toolTip = file.getDescription();
				  }
				} else {
				  label = null;
				}
			}
		}
		
		if (label != null) {
		  updateLabel(label, icon, toolTip, sel, tree.hasFocus());
		}
    return label;
  }

  /**
   * Update label.
   * 
   * @param label          The label to update.
   * @param icon           The icon to set to the label.
   * @param toolTip        The tooltip text to set to the label.
   * @param isSelected     <code>true</code> if the label is selected.
   * @param isTreeFocused  <code>true</code> if the tree is focused.
   */
  private void updateLabel(JLabel label, Icon icon, String toolTip, boolean isSelected, boolean isTreeFocused) {
    label.setIcon(icon);
    label.setToolTipText(toolTip);

    if (isSelected) {
      if (isTreeFocused) {
        setBackgroundSelectionColor(defaultSelectionColor);
      } else if (!contextMenuShowing.getAsBoolean()) {
        // Do nor render the tree as inactive if we have a contextual menu over it.
        setBackgroundSelectionColor(RendererUtil.getInactiveSelectionColor());
      }
    }
  }
  
}