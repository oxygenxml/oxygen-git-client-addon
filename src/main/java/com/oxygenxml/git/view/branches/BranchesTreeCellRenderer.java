package com.oxygenxml.git.view.branches;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.history.RoundedLineBorder;
import com.oxygenxml.git.view.util.RendererUtil;
import com.oxygenxml.git.view.util.RenderingInfo;
import com.oxygenxml.git.view.util.UIUtil;

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
  private static final Translator TRANSLATOR = Translator.getInstance();
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
   * Logger for logging.
   */
  private static final Logger LOGGER = LogManager.getLogger(BranchesTreeCellRenderer.class.getName());
  /**
   * Date format.
   */
  private final SimpleDateFormat dateFormat = new SimpleDateFormat(UIUtil.DATE_FORMAT_PATTERN);

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
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

    JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

    if(label == null) {
      return null;
    }
      
    Icon icon = null;
    String text = "";
    String path = value.toString();
    if (((DefaultMutableTreeNode) value).getParent() == null) {
      icon = Icons.getIcon(Icons.LOCAL_REPO);
    } else {
      RenderingInfo renderingInfo = getRenderingInfo(path);
      icon = renderingInfo.getIcon();
      text = renderingInfo.getTooltip();
    }
    
    label.setIcon(icon);
    if (!text.isEmpty()) {
      label.setText(text);
      try {
        String toolTipText = computeToolTipText(leaf, path, text);
        label.setToolTipText(toolTipText);
      } catch (GitAPIException | IOException | NoRepositorySelected e) {
        LOGGER.error(e, e);
      }
    }
      Font font = label.getFont();
      label.setFont(font.deriveFont(Font.PLAIN));
      label.setBorder(new EmptyBorder(0, 5, 0, 0));
      if (path.equals(Constants.R_HEADS + currentBranchNameSupplier.get())) {
        // Mark the current branch
        label.setFont(font.deriveFont(Font.BOLD));
        label.setBorder(new RoundedLineBorder(label.getForeground(), 1, CURRENT_BRANCH_BORDER_CRONER_SIZE, true));
      }
      // Active/inactive table selection
      if (sel) {
        setSelectionColors(tree);
      }
    return label;
  }

  /**
   * @param leaf <code>true<code> if is leaf
   * @param path the path
   * @param text the extracted text
   * 
   * @return the computed tooltip.
   * 
   * @throws GitAPIException
   * @throws IOException
   * @throws NoRepositorySelected
   */
  private String computeToolTipText(boolean leaf, String path, String text) throws GitAPIException, IOException, NoRepositorySelected {
    String toolTipText = "";
    if (GitAccess.getInstance().isRepoInitialized() && leaf) {
      if(path.contains(Constants.R_REMOTES)) {
        toolTipText = constructRemoteBranchToolTip(text, path);
      } else if (path.contains(Constants.R_HEADS)) {
        String branchName = BranchesUtil.createBranchPath(
            path,
            BranchManagementConstants.LOCAL_BRANCH_NODE_TREE_LEVEL);
        if(BranchesUtil.existsLocalBranch(branchName)) {
          toolTipText = constructLocalBranchToolTip(branchName);
        }    
      }
    }
    return toolTipText;
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
      setBorderSelectionColor(RendererUtil.getInactiveSelectionColor(tree, defaultSelectionColor));
      setBackgroundSelectionColor(RendererUtil.getInactiveSelectionColor(tree, defaultSelectionColor));
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
  
  /**
   * Construct message for local branches.
   * 
   * @param nameBranch name of the branch.
   * 
   * @return the message.
   * 
   * @throws GitAPIException
   * @throws IOException
   * @throws NoRepositorySelected 
   */
  private String constructLocalBranchToolTip(String nameBranch) throws GitAPIException, IOException, NoRepositorySelected {
    StringBuilder toolTipText = new StringBuilder();
    PersonIdent authorDetails = GitAccess.getInstance().getLatestCommitForBranch(nameBranch).getAuthorIdent();
    String remoteBranchName = GitAccess.getInstance().getUpstreamBranchShortNameFromConfig(nameBranch);
    boolean foundRemoteBranch = remoteBranchName != null;
    toolTipText.append("<html><p>")
      .append(TRANSLATOR.getTranslation(Tags.LOCAL_BRANCH))
      .append(" ")
      .append(nameBranch);
    if(foundRemoteBranch) {
      toolTipText.append("<br>")
        .append(TRANSLATOR.getTranslation(Tags.UPSTREAM_BRANCH))
        .append(" ")
        .append(remoteBranchName);
    }
    toolTipText.append("<br>")
      .append("<br>")
      .append(TRANSLATOR.getTranslation(Tags.LAST_COMMIT_DETAILS))
      .append(":<br>- ")
      .append(TRANSLATOR.getTranslation(Tags.AUTHOR))
      .append(": ")
      .append(authorDetails.getName())
      .append(" &lt;")
      .append(authorDetails.getEmailAddress())
      .append("&gt;<br> - ")
      .append(TRANSLATOR.getTranslation(Tags.DATE))
      .append(": ")
      .append(dateFormat.format(authorDetails.getWhen()))
      .append("</p></html>");
    return toolTipText.toString();
  }
  
  /**
   * Construct message for remote branches.
   * 
   * @param branchName name of the branch.
   * @param path       the location of the branch.
   * 
   * @return the message.
   * 
   * @throws GitAPIException
   * @throws IOException
   * @throws NoRepositorySelected
   */
  private String constructRemoteBranchToolTip(String branchName, String path) throws GitAPIException, IOException, NoRepositorySelected {
    StringBuilder toolTipText = new StringBuilder();
    PersonIdent authorDetails = GitAccess.getInstance().getLatestCommitForBranch(path).getAuthorIdent();
    String remoteURL = GitAccess.getInstance().getRemoteURLFromConfig();
    toolTipText.append("<html><p>")
      .append(TRANSLATOR.getTranslation(Tags.REMOTE_BRANCH))
      .append(" ")
      .append(Constants.DEFAULT_REMOTE_NAME)
      .append("/")
      .append(branchName)
      .append("<br>")
      .append(TRANSLATOR.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_URL_LABEL))
      .append(": ")
      .append("<a href=\"" + remoteURL + "\">")
      .append(remoteURL + "</a>")
      .append("<br>")
      .append("<br>")
      .append(TRANSLATOR.getTranslation(Tags.LAST_COMMIT_DETAILS))
      .append(":<br>- ")
      .append(TRANSLATOR.getTranslation(Tags.AUTHOR))
      .append(": ")
      .append(authorDetails.getName())
      .append(" &lt;")
      .append(authorDetails.getEmailAddress())
      .append("&gt;<br> - ")
      .append(TRANSLATOR.getTranslation(Tags.DATE))
      .append(": ")
      .append(dateFormat.format(authorDetails.getWhen()))
      .append("</p></html>");
    return toolTipText.toString();
  }
  
}
