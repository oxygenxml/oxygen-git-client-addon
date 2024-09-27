package com.oxygenxml.git.view.remotes;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URISyntaxException;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.annotation.TestOnly;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.branches.BranchConfigurations;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;


/**
 * Dialog used to edit the current branch remote.
 * 
 * @author alex_smarandache
 *
 */
public class CurrentBranchRemotesDialog extends OKCancelDialog {

  /**
   * The translator.
   */
  private static final Translator TRANSLATOR = Translator.getInstance();

  /**
   * The default dialog width.
   */
  private static final int DIALOG_WIDTH = 550;

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(CurrentBranchRemotesDialog.class);

  /**
   * Combo box with all remotes from current repository.
   */
  private final JComboBox<RemoteBranchItem> remoteBranchItems = new JComboBox<>();

  /**
   * The current branch.
   */
  private final String currentBranch;

  /**
   * The current dialog status.
   */
  private int currentStatus = RemotesViewUtil.STATUS_REMOTE_OK;

  /**
   * Constructor.
   */
  public CurrentBranchRemotesDialog() {
    super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
        TRANSLATOR.getTranslation(Tags.CONFIGURE_REMOTE_FOR_BRANCH), true
        );

    setOkButtonText(TRANSLATOR.getTranslation(Tags.TRACK_BRANCH));
    currentBranch = GitAccess.getInstance().getBranchInfo().getBranchName();

    try {
      RemotesViewUtil.installRemoteBranchesRenderer(remoteBranchItems);
      currentStatus = RemotesViewUtil.addRemoteBranches(remoteBranchItems, currentBranch);
    } catch (NoRepositorySelected | URISyntaxException e) {
      LOGGER.error(e.getMessage(), e);
    }

    if(currentStatus == RemotesViewUtil.STATUS_REMOTE_NOT_EXISTS || currentStatus == RemotesViewUtil.STATUS_BRANCHES_NOT_EXIST) {
      this.doCancel();
    } else if(currentStatus == RemotesViewUtil.STATUS_REMOTE_OK){
      getContentPane().add(createGUIPanel());

      pack();
      repaint();

      JFrame parentFrame = PluginWorkspaceProvider.getPluginWorkspace() != null ? 
          (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null;
      if (parentFrame != null) {
        setIconImage(parentFrame.getIconImage());
        setLocationRelativeTo(parentFrame);
      }

      setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

      this.setVisible(true);
      this.setResizable(false);
    }
  }


  @Override
  public Dimension getPreferredSize() {
    final Dimension prefferedSize = super.getPreferredSize();
    return new Dimension(Math.max(DIALOG_WIDTH, prefferedSize.width), prefferedSize.height);
  }


  /**
   * Create the dialog GUI.
   * 
   * @return The created panel.
   */
  private JPanel createGUIPanel() {
    JPanel guiPanel = new JPanel(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();

    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_LARGE_PADDING);
    constraints.weightx = 0;
    constraints.weighty = 0;
    constraints.fill = GridBagConstraints.NONE;

    JLabel remoteNameLabel = new JLabel(TRANSLATOR.getTranslation(Tags.LOCAL_BRANCH));
    guiPanel.add(remoteNameLabel, constraints);

    constraints.gridx++;
    guiPanel.add(new JLabel(currentBranch), constraints);

    constraints.gridx = 0;
    constraints.gridy++;
    guiPanel.add(new JLabel(TRANSLATOR.getTranslation(Tags.REMOTE_TRACKING_BRANCH) + ":"), constraints);

    constraints.weightx = 1;
    constraints.gridx++;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, 0);
    guiPanel.add(remoteBranchItems, constraints);

    return guiPanel;
  }


  @Override
  protected void doOK() {
    RemoteBranchItem currentSelectedBranch = (RemoteBranchItem) remoteBranchItems.getSelectedItem();
    if(!currentSelectedBranch.isUndefined() && !currentSelectedBranch.isFirstSelection()) {
      try {
        BranchConfigurations branchConfig = new BranchConfigurations(
            GitAccess.getInstance().getRepository().getConfig(), currentBranch);
        branchConfig.setRemote(currentSelectedBranch.remote);
        branchConfig.setMerge(currentSelectedBranch.branch);
        GitAccess.getInstance().updateConfigFile();
      } catch (NoRepositorySelected e) {
        LOGGER.error(e.getMessage(), e);
      }
    }

    super.doOK();
  }


  /**
   * @return The dialog status.
   */
  public int getStatusResult() {
    return currentStatus;
  }


  /**
   * @return The remote branch items.
   */
  @TestOnly
  public JComboBox<RemoteBranchItem> getRemoteBranchItems() {
    return remoteBranchItems;
  }

}
