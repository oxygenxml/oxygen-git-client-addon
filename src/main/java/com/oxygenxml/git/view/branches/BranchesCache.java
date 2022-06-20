package com.oxygenxml.git.view.branches;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.util.UIUtil;


/**
 * 
 * TODO Asta nu e cumva un Branch Tooltip Cacher?
 * 
 * A cache for branches details.
 * 
 * @author alex_smarandache
 */
public class BranchesCache {

	/**
	 * The translator.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(BranchesCache.class);

	/**
	 * A map where: <code>key</code>: the node value, <code>value</code>: the tool tip for node. 
	 */
	private final Map<String, String> nodesTooltips = new HashMap<>();


	/**
	 * @param leaf <code>true<code> if is leaf
	 * @param path the branch path
	 * @param branchName the branch name
	 * 
	 * @return The computed tool tip text.
	 * 
	 * @throws NoRepositorySelected 
	 * @throws IOException 
	 * @throws GitAPIException 
	 */
	public String getToolTip(boolean leaf, String path, String branchName) {
		return !branchName.isEmpty() ? nodesTooltips.computeIfAbsent(path, s -> {
			try {
				return ToolTipContentProvider.computeToolTipText(leaf, path, branchName);
			} catch (GitAPIException | IOException | NoRepositorySelected e) {
				LOGGER.error(e.getMessage(), e);
				return null;
			}
		}) : null;
	}


	/**
	 * Reset the cache.
	 */
	public void reset() {
		nodesTooltips.clear();
	}


	/**
	 * Inner class with generate tool tip responsibility.
	 * 
	 * @author alex_smarandache
	 *
	 */
	private static final class ToolTipContentProvider {

		/**
		 * Compute tooltip text.
		 * 
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
		static String computeToolTipText(boolean leaf, String path, String text) throws GitAPIException, IOException, NoRepositorySelected {
			String toolTipText = null;
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
		private static String constructLocalBranchToolTip(String nameBranch) throws GitAPIException, IOException, NoRepositorySelected {
			StringBuilder toolTipText = new StringBuilder();
			final SimpleDateFormat dateFormat = new SimpleDateFormat(UIUtil.DATE_FORMAT_PATTERN);
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
		private static String constructRemoteBranchToolTip(String branchName, String path) throws GitAPIException, IOException, NoRepositorySelected {
			StringBuilder toolTipText = new StringBuilder();
			final SimpleDateFormat dateFormat = new SimpleDateFormat(UIUtil.DATE_FORMAT_PATTERN);
			PersonIdent authorDetails = GitAccess.getInstance().getLatestCommitForBranch(path).getAuthorIdent();
			String[] pathDetails = path.split("/");
			String remoteURL = GitAccess.getInstance().getRemoteURLFromConfig(pathDetails[2]);
			toolTipText.append("<html><p>")
			.append(TRANSLATOR.getTranslation(Tags.REMOTE_BRANCH))
			.append(" ")
			.append(pathDetails[2])
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



}
