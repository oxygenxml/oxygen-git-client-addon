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
   * Constant for status when the remote exists.
   */
  public static final int STATUS_REMOTE_OK = 0;
  
  /**
   * Constant for status when the remote doesn't exists.
   */
  public static final int STATUS_REMOTE_NOT_EXISTS = 1;

  /**
   * Constant for status when branches are not founded.
   */
  public static final int STATUS_BRANCHES_NOT_EXIST = 2;


  /**
   * Install the renderer to present the remote branches in combo.
   * 
   * @param remoteBranchItems The combobox with remote branches.
   */
  public static void installRemoteBranchesRenderer(JComboBox<RemoteBranch> remoteBranchItems) {
    final ListCellRenderer<? super RemoteBranch> oldRender = remoteBranchItems.getRenderer();
    remoteBranchItems.setRenderer((list, value, index, isSelected, cellHasFocus) -> {

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
   * @param remoteBranchItems  The combobox with remote branches.
   * @param currentBranch      The name of the current branch.
   *
   * @return The current status of the operation. <br><b>RemoteViewUtil.STATUS_REMOTE_OK</b> - if the remote are added successfully.
   * <br><b>RemoteViewUtil.STATUS_REMOTE_NOT_EXISTS</b> - when the remote doesn't exists.
   * <br><b>RemoteViewUtil.STATUS_BRANCHES_NOT_EXIST</b> - when branches are not founded.
   * 
   * @throws URISyntaxException
   * @throws NoRepositorySelected 
   */
  public static int addRemoteBranches(
      final JComboBox<RemoteBranch> remoteBranchItems, 
      final String currentBranch) throws URISyntaxException, NoRepositorySelected {
    final List<RemoteBranch> branchesToAdd = new ArrayList<>();
    final StoredConfig config = GitAccess.getInstance().getRepository().getConfig();
    final BranchConfigurations branchConfig = new BranchConfigurations(config, currentBranch);
    final List<String> remotesNames = new ArrayList<>(GitAccess.getInstance().getRemotesFromConfig().keySet());
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
          remoteItem.setFirstSelection(true);
          branchesToAdd.add(remoteItem);
        } else {
          branchesToAdd.add(new RemoteBranch(remote, branchName));
        }
      }
    }

    if(!foundBranchRemoteForCurrentLocalBranch) {
      final RemoteBranch remoteItem = new RemoteBranch(null, null);
      remoteItem.setFirstSelection(true);
      remoteBranchItems.addItem(remoteItem);    
      remoteBranchItems.setSelectedIndex(remoteBranchItems.getItemCount() - 1);
    }

    int currentStatus;
    if (remotesNames.isEmpty()) {
      currentStatus = STATUS_REMOTE_NOT_EXISTS;
    } else if (branchesToAdd.isEmpty()) {
      currentStatus = STATUS_BRANCHES_NOT_EXIST;
    } else {
      currentStatus = STATUS_REMOTE_OK;
      branchesToAdd.sort((b1, b2) -> {
        int comparasionResult = !b1.isUndefined() && !b2.isUndefined() ? 
            Boolean.compare(b2.branchFullName.endsWith(currentBranch), b1.branchFullName.endsWith(currentBranch)) 
              : 0;
        if(comparasionResult == 0) {
          comparasionResult = b1.toString().compareTo(b2.toString());
        }
        return comparasionResult;
      });

      branchesToAdd.forEach(branch -> {
        remoteBranchItems.addItem(branch);
        if(branch.isFirstSelection()) {
          remoteBranchItems.setSelectedIndex(remoteBranchItems.getItemCount() - 1);
        }
      });
    }
    
    return currentStatus;
  }

}
