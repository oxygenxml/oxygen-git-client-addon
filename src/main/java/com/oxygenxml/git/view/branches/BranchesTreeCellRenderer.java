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
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.eclipse.jgit.lib.Constants;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.view.RoundedLineBorder;
import com.oxygenxml.git.view.util.RendererUtil;
import com.oxygenxml.git.view.util.RenderingInfo;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.util.ColorTheme;

/**
 * Renderer for the nodes icon in the branches tree, based on the path to the
 * node.
 *
 * @author Bogdan Draghici
 *
 */
public class BranchesTreeCellRenderer extends DefaultTreeCellRenderer {

  /**
   * The corner size for the current branch border.
   */
  private static final int CURRENT_BRANCH_BORDER_CRONER_SIZE = 8;
  /**
   * Default selection color.
   */
  private final Color defaultSelectionColor = getBackgroundSelectionColor();
  /**
   * Default border color for selection.
   */
  private final Color defaultBorderSelectionColor = new Color(102,167,232);
  /**
   * Tells us if the context menu is showing.
   */
  private final BooleanSupplier isContextMenuShowing;
  /**
   * Supplies the current branch.
   */
  private final Supplier<String> currentBranchNameSupplier;
  
  /**
   * Cache with information about nodes.  
   */
  private final transient BranchesTooltipsCache cache;

  /**
   * The branch name.
   */
  private String branchName = null;
  
  /**
   * The branch path.
   */
  private String path = null;
  
  /**
   * <code>true</code> if node is a leaf.
   */
  private boolean isLeaf = false;
  
  
  /**
   * Constructor.
   *
   * @param isContextMenuShowing Tells us if the context menu is showing.
   * @param currentBranchNameSupplier Gives us the current branch name.
   */
  public BranchesTreeCellRenderer(
	  BranchesTooltipsCache cache,
      BooleanSupplier isContextMenuShowing,
      Supplier<String> currentBranchNameSupplier) {
	this.cache = cache;
    this.isContextMenuShowing = isContextMenuShowing;
    this.currentBranchNameSupplier = currentBranchNameSupplier;
  }

  
  /**
   * @see DefaultTreeCellRenderer.getTreeCellRendererComponent(JTree, Object, boolean, boolean, boolean, int, boolean)
   */
  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

    JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

    if(label == null) {
      return null;
    }
      
    Icon icon = null;
    branchName = "";
    this.isLeaf = leaf;
    path = value.toString();
    if (((DefaultMutableTreeNode) value).getParent() == null) {
      icon = Icons.getIcon(Icons.LOCAL_REPO);
    } else {
      RenderingInfo renderingInfo = getRenderingInfo(path);
      icon = renderingInfo.getIcon();
      branchName = renderingInfo.getTooltip();
    }
    
    this.setIcon(icon);
    if (!branchName.isEmpty()) {
      this.setText(branchName);
    }
      Font font = label.getFont();
      this.setFont(font.deriveFont(Font.PLAIN));
      this.setBorder(new EmptyBorder(0, 5, 0, 0));
      if (path.equals(Constants.R_HEADS + currentBranchNameSupplier.get())) {
        // Mark the current branch
        this.setFont(font.deriveFont(Font.BOLD));
        this.setBorder(new RoundedLineBorder(label.getForeground(), 1, CURRENT_BRANCH_BORDER_CRONER_SIZE, true));
      }
      // Active/inactive table selection
      if (sel) {
        setSelectionColors(tree);
      }
    return this;
  }

  
  /**
   * Get the rendering info (such as icon) for the given branch.
   *
   * @param branchPath The string path to the branch.
   *
   * @return The rendering info.
   */
  private RenderingInfo getRenderingInfo(String branchPath) {
    RenderingInfo renderingInfo = null;
    if (branchPath.equals(Constants.R_HEADS)) {
      renderingInfo = new RenderingInfo(Icons.getIcon(Icons.LOCAL), BranchManagementConstants.LOCAL);
    } else if (branchPath.equals(Constants.R_REMOTES)) {
      renderingInfo = new RenderingInfo(Icons.getIcon(Icons.REMOTE), BranchManagementConstants.REMOTE);
    } else {
      String[] split = branchPath.split("/");
      renderingInfo = new RenderingInfo(null, split[split.length - 1]);
    }
    return renderingInfo;
  }
  

  /**
   * Set selection background and border colors.
   *
   * @param tree The tree.
   */
  private void setSelectionColors(JTree tree) {
    if (tree.hasFocus()) {
      ColorTheme colorTheme = PluginWorkspaceProvider.getPluginWorkspace().getColorTheme();
      setBorderSelectionColor(colorTheme.isDarkTheme() ? defaultSelectionColor
              : defaultBorderSelectionColor);
      setBackgroundSelectionColor(defaultSelectionColor);
    } else if (!isContextMenuShowing.getAsBoolean()) {
      setBorderSelectionColor(RendererUtil.getInactiveSelectionColor());
      setBackgroundSelectionColor(RendererUtil.getInactiveSelectionColor());
    }
  }
  

  /**
   * Paints the node, and in case it is also selected, take care not to draw the dashed rectangle border.
   * @see com.oxygenxml.git.view.branches.BranchesTreeCellRenderer.paint(Graphics)
   */
  @Override
  public void paint(Graphics g) {
    if (selected) {
      g.setColor(getBackgroundSelectionColor());
      g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
      hasFocus = false;
      super.paint(g);
      paintBorder(g, 0, 0, getWidth(), getHeight());
    } else {
      super.paint(g);
    }
  }

  
  /**
   * Paints the border for the selected node.
   *
   * @param g Graphics.
   * @param x X coordinate.
   * @param y Y coordinate.
   * @param w Width.
   * @param h Height.
   */
  private void paintBorder(Graphics g, int x, int y, int w, int h) {
    Color bsColor = getBorderSelectionColor();
    if (bsColor != null && selected) {
      g.setColor(bsColor);
      g.drawRect(x, y, w - 1, h - 1);
    }
  }
 
  @Override
	public String getToolTipText() {
		return cache != null ? cache.getToolTip(isLeaf, path, branchName) : super.getToolTipText();
	}
  
}
