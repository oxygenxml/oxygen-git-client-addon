package com.oxygenxml.git.view.remotes;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;

import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.URIish;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.service.exceptions.RemoteNotFoundException;
import com.oxygenxml.git.utils.TextFormatUtil;
import com.oxygenxml.git.view.branches.BranchConfigurations;

/**
 * Contains usefully methods to present the remote branches in a component. 
 * 
 * @author alex_smarandache
 */
public class RemotesViewUtil {
  
  /**
   * Maximum number of characters for a remote branch item.
   */
  private static final int MAXIMUM_REMOTE_ITEM_NO_OF_CHARACTERS = 60;

  /**
   * Install the renderer to present the remote branches in combo.
   * 
   * @param remoteBranchCombo The combobox with remote branches.
   */
  public static void installRemoteBranchesRenderer(JComboBox<RemoteBranch> remoteBranchCombo) {
    final ListCellRenderer<? super RemoteBranch> oldRender = remoteBranchCombo.getRenderer();
    remoteBranchCombo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {

      final JLabel toReturn = (JLabel) 
          oldRender.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      final Border padding = BorderFactory.createEmptyBorder(
          0, 
          UIConstants.COMPONENT_LEFT_PADDING, 
          0, 
          UIConstants.COMPONENT_RIGHT_PADDING
          );
      toReturn.setBorder(padding);
      
      toReturn.setText(
          TextFormatUtil.shortenText(
              toReturn.getText(),
              MAXIMUM_REMOTE_ITEM_NO_OF_CHARACTERS, 
              0,
              "..."));

      return toReturn;
    });
  }
  
  /**
   * Add remote branches for current repository. 
   * 
   * @param remoteBranchCombo  The combobox with remote branches.
   * @param currentBranch      The name of the current branch.
   *
   * @return The current status of the operation. <br><b>RemoteViewUtil.STATUS_REMOTE_OK</b> - if the remote are added successfully.
   * <br><b>RemoteViewUtil.STATUS_REMOTE_NOT_EXISTS</b> - when the remote doesn't exists.
   * <br><b>RemoteViewUtil.STATUS_BRANCHES_NOT_EXIST</b> - when branches are not founded.
   * 
   * @throws URISyntaxException
   * @throws NoRepositorySelected 
   * 
   * @throws RemoteNotFoundException  When no remote or branch remote is found.
   */
  public static void addRemoteBranches(
      final JComboBox<RemoteBranch> remoteBranchCombo, 
      final String currentBranch) throws URISyntaxException, NoRepositorySelected, RemoteNotFoundException {
    final List<RemoteBranch> remoteBranches = new ArrayList<>();
    final StoredConfig config = GitAccess.getInstance().getRepository().getConfig();
    final BranchConfigurations branchConfig = new BranchConfigurations(config, currentBranch);
    final List<String> remotesNames = new ArrayList<>(GitAccess.getInstance().getRemotesFromConfig().keySet());
    
    boolean foundBranchRemoteForCurrentLocalBranch = appendRemoteNames(
        remoteBranches,
        config,
        branchConfig,
        remotesNames);
    if(!foundBranchRemoteForCurrentLocalBranch) {
      addUndefinedRemoteBranchForCurrentLocal(remoteBranchCombo);
    }

    if (remotesNames.isEmpty()) {
      throw new RemoteNotFoundException(RemoteNotFoundException.STATUS_REMOTE_NOT_EXISTS);
    } else if (remoteBranches.isEmpty()) {
      throw new RemoteNotFoundException(RemoteNotFoundException.STATUS_BRANCHES_NOT_EXIST);
    } else {
      sortBranchesAlphabetically(currentBranch, remoteBranches);
      addRemoteBranchesInCombo(remoteBranchCombo, remoteBranches);
    }
  }

  /**
   * Add the remote branches as items in the combobox.
   * 
   * @param remoteBranchesCombo  The remote branches combobox.
   * @param remoteBranches       The remote branches.  
   */
  private static void addRemoteBranchesInCombo(
      final JComboBox<RemoteBranch> remoteBranchesCombo,
      final List<RemoteBranch> remoteBranches) {
    remoteBranches.forEach(branch -> {
      remoteBranchesCombo.addItem(branch);
      if(branch.isCurrentBranch()) {
        remoteBranchesCombo.setSelectedIndex(remoteBranchesCombo.getItemCount() - 1);
      }
    });
  }

  /**
   * Sort the given remote branches list alphabetically.
   * 
   * @param currentBranch   The current  branch.
   * @param remoteBranches  The remote branches
   */
  private static void sortBranchesAlphabetically(final String currentBranch, final List<RemoteBranch> remoteBranches) {
    remoteBranches.sort((b1, b2) -> {
      int comparasionResult;
      if (RemoteBranch.UNDEFINED_BRANCH.equals(b1) || RemoteBranch.UNDEFINED_BRANCH.equals(b2)) {
        comparasionResult = b1.toString().compareTo(b2.toString());
      } else {
        comparasionResult = Boolean.compare(
            b2.getBranchFullName().endsWith(currentBranch),
            b1.getBranchFullName().endsWith(currentBranch)); 
      }
      return comparasionResult;
    });
  }

  /**
   * This method must be called only when the current local branch has no remote branch associated. Thus, a undefined branch will be paired with the local branch as its remote branch.
   * 
   * @param remoteBranchItems   The current remote branches.
   */
  private static void addUndefinedRemoteBranchForCurrentLocal(final JComboBox<RemoteBranch> remoteBranchItems) {
    final RemoteBranch remoteItem = new RemoteBranch(null, null);
    remoteItem.setIsCurrentBranch(true);
    remoteBranchItems.addItem(remoteItem);    
    remoteBranchItems.setSelectedIndex(remoteBranchItems.getItemCount() - 1);
  }

  /**
   * 
   * @param branchesToAdd   The branches to be added.
   * @param config          Configuration of the repository.
   * @param branchConfig    The configuration of the branch.
   * @param remotesNames    The names for remote branches.
   * 
   * @return <code>true</code> if the current local branch has a remote branch.
   * 
   * @throws URISyntaxException When a problem occurs.
   */
  private static boolean appendRemoteNames(
      final List<RemoteBranch> branchesToAdd, 
      final StoredConfig config,
      final BranchConfigurations branchConfig, 
      final List<String> remotesNames) throws URISyntaxException {
    boolean foundBranchRemoteForCurrentLocalBranch = false;

    for(String remote : remotesNames) {
      final URIish sourceURL = new URIish(
          config.getString(
              ConfigConstants.CONFIG_REMOTE_SECTION,
              remote,
              ConfigConstants.CONFIG_KEY_URL));
      final Collection<Ref> branchesConfig = GitAccess.getInstance().doListRemoteBranchesInternal(sourceURL, null);

      for(Ref branch: branchesConfig) {
        final String branchName = branch.getName();
        final String remoteFromConfig = branchConfig.getRemote();
        final String mergeFromConfig = branchConfig.getMerge();
        if(remoteFromConfig != null && remoteFromConfig.equals(remote) 
            && mergeFromConfig != null && mergeFromConfig.equals(branchName)) {
          final RemoteBranch remoteItem = new RemoteBranch(remote, branchName);
          foundBranchRemoteForCurrentLocalBranch = true;
          remoteItem.setIsCurrentBranch(true);
          branchesToAdd.add(remoteItem);
        } else {
          branchesToAdd.add(new RemoteBranch(remote, branchName));
        }
      }
    }
    
    return foundBranchRemoteForCurrentLocalBranch;
  }

}
