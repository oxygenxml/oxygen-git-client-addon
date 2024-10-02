package com.oxygenxml.git.view.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

import org.eclipse.jgit.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.annotation.TestOnly;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.service.exceptions.RemoteNotFoundException;
import com.oxygenxml.git.service.internal.PullConfig;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.remotes.CurrentBranchRemotesDialog;
import com.oxygenxml.git.view.remotes.RemoteBranch;
import com.oxygenxml.git.view.remotes.RemotesRepositoryDialog;
import com.oxygenxml.git.view.remotes.RemotesViewUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;

/**
 * The dialog corresponding to the advanced pull that can be configured.
 */
public class AdvancedPullDialog extends OKCancelDialog {

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
  private final JComboBox<RemoteBranch> remoteBranchItems = new JComboBox<>();

  /**
   * The combobox for pull types.
   */
  private final JComboBox<PullType> pullTypesCombo = new JComboBox<>();

  /**
   * The current branch.
   */
  private final String currentBranch;
  
  /**
   * The configuration of the pull operation.
   */
  private PullConfig pullConfig;

  /**
   * The Git controller.
   */
  private final GitController gitCtrl;
  
  /**
   * Constructor.
   */
  public AdvancedPullDialog(final GitController gitCtrl) {
    super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
        TRANSLATOR.getTranslation(Tags.PULL),
        true);
    
    this.gitCtrl = gitCtrl;
    currentBranch = gitCtrl.getGitAccess().getBranchInfo().getBranchName();
  }


  /**
   * This message shows the dialog.
   * 
   * @throws RemoteNotFoundException When the remote repository or branches cannot be found.
   */
  public void showDialog() throws RemoteNotFoundException {
    
    setOkButtonText(TRANSLATOR.getTranslation(Tags.PULL_CHANGES));

    RemotesViewUtil.installRemoteBranchesRenderer(remoteBranchItems);
    loadRemotesRepositories();

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

    this.setResizable(false);
    setVisible(true);
  }


  /**
   * This method clear the remote branches combo box and reload it.
   * 
   * @throws RemoteNotFoundException This exception appear when a remote is not found.
   */
  private void loadRemotesRepositories() throws RemoteNotFoundException {
    remoteBranchItems.removeAllItems();
    try {
      RemotesViewUtil.addRemoteBranches(remoteBranchItems, currentBranch);
    } catch (NoRepositorySelected | URISyntaxException e) {
      LOGGER.error(e.getMessage(), e);
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
    GridBagConstraints gbc = new GridBagConstraints();

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_LARGE_PADDING);
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.fill = GridBagConstraints.NONE;

    JLabel remoteNameLabel = new JLabel(TRANSLATOR.getTranslation(Tags.LOCAL_BRANCH));
    guiPanel.add(remoteNameLabel, gbc);

    gbc.gridx++;
    guiPanel.add(new JLabel(currentBranch), gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    guiPanel.add(new JLabel(TRANSLATOR.getTranslation(Tags.PULL_FROM_REMOTE_BRANCH) + ":"), gbc);

    gbc.weightx = 1;
    gbc.gridx++;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, 0);
    guiPanel.add(createManageRemotesPanel(), gbc);

    pullTypesCombo.addItem(PullType.MERGE_FF);
    pullTypesCombo.addItem(PullType.REBASE);
    pullTypesCombo.setSelectedItem(OptionsManager.getInstance().getDefaultPullType());
    final ListCellRenderer<? super PullType> oldRender = pullTypesCombo.getRenderer();
    pullTypesCombo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {

      final JLabel toReturn = (JLabel) 
          oldRender.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      final Border padding = BorderFactory.createEmptyBorder(
          0, 
          UIConstants.COMPONENT_LEFT_PADDING, 
          0, 
          UIConstants.COMPONENT_RIGHT_PADDING
          );

      toReturn.setBorder(padding);
      toReturn.setText(TRANSLATOR.getTranslation(value == PullType.REBASE ? Tags.REBASE : Tags.MERGE_LOWERCASE));

      return toReturn;
    });

    gbc.gridx = 0;
    gbc.gridwidth = 1;
    gbc.gridy++;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0;
    guiPanel.add(new JLabel(TRANSLATOR.getTranslation(Tags.PULL_TYPE) + ":"), gbc);

    gbc.weightx = 1;
    gbc.gridx++;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, 0);
    guiPanel.add(pullTypesCombo, gbc);

    return guiPanel;
  }


  /**
   * @return A panel with the remote branches.
   */
  private JPanel createManageRemotesPanel() {
    JPanel manageRemotesPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    manageRemotesPanel.add(remoteBranchItems, gbc);

    gbc.gridx++;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.insets = new Insets(0, UIConstants.COMPONENT_LEFT_PADDING, 0, 0);
    AbstractAction manageRemotesAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          if (gitCtrl.getGitAccess().getRepository() != null) {
            RemotesRepositoryDialog remotesRepoDialog = new RemotesRepositoryDialog();
            remotesRepoDialog.configureRemotes();
            if(remotesRepoDialog.getResult() == OKCancelDialog.RESULT_OK) {
              try {
                loadRemotesRepositories();
              } catch (RemoteNotFoundException ex) {
                LOGGER.debug(ex.getMessage(), ex);
              }
            }
          }
        } catch (NoRepositorySelected e1) {
          if(LOGGER.isDebugEnabled()) {
            LOGGER.debug(e1.getMessage(), e1);
          }
        } 
      }
    };
    manageRemotesAction.putValue(Action.SMALL_ICON, Icons.getIcon(Icons.REMOTE));
    ToolbarButton manageRemotesBtn = new ToolbarButton(manageRemotesAction, false);
    manageRemotesBtn.setToolTipText(TRANSLATOR.getTranslation(Tags.MANAGE_REMOTE_REPOSITORIES) + "...");
    manageRemotesPanel.add(manageRemotesBtn, gbc);

    return manageRemotesPanel;
  }


  @Override
  protected void doOK() {
    RemoteBranch currentSelectedBranch = (RemoteBranch) remoteBranchItems.getSelectedItem();
    if(!RemoteBranch.UNDEFINED_BRANCH.equals(currentSelectedBranch)) {
      pullConfig = PullConfig
          .builder()
          .branchName(Optional.of(currentSelectedBranch.getBranchFullName()))
          .remote(Optional.of(currentSelectedBranch.getRemote()))
          .pullType((PullType) pullTypesCombo.getSelectedItem())
          .updateSubmodule(OptionsManager.getInstance().getUpdateSubmodulesOnPull())
          .build();
    }

    super.doOK();
  }

  /**
   * @return The configuration of the pull operation.
   */
  @Nullable
  public PullConfig getPullConfig() {
    return pullConfig;
  }
  
  /**
   * @return The remote branch items.
   */
  @TestOnly
  public JComboBox<RemoteBranch> getRemoteBranchItems() {
    return remoteBranchItems;
  }

}
