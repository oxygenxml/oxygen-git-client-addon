package com.oxygenxml.git;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.FileHelper;

import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.ui.Icons;

/**
 * InvocationHandler for ProjectPopupMenuCustomizer
 * 
 * @author Beniamin Savu
 *
 */
public class ProjectPopupMenuCustomizerInvocationHandler implements java.lang.reflect.InvocationHandler {
	private JMenu git;
	private Object plugin;

	public ProjectPopupMenuCustomizerInvocationHandler(JMenu git, Object plugin) {
		this.git = git;
		this.plugin = plugin;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object result = null;
		try {

			// if the method name equals with "customizePopUpMenu"
			if (method.getName().equals("customizePopUpMenu")) {
				boolean isGit = false;
				String repository = null;
				// cast the args[0] at JPopupMenu
				JPopupMenu popupMenu = (JPopupMenu) args[0];

				// check if the selected files are git files
				File[] selectedFiles = ProjectManagerEditor.getSelectedFiles((StandalonePluginWorkspace) plugin);
				for (int i = 0; i < selectedFiles.length; i++) {
					isGit = false;
					File temp = new File(selectedFiles[i].getAbsolutePath());
					while (temp.getParent() != null && !isGit) {
						if (FileHelper.isGitRepository(temp.getPath())) {
							repository = temp.getPath();
							isGit = true;
						}
						temp = temp.getParentFile();
					}
					if (isGit == false) {
						break;
					}
				}

				if (isGit) {
				  // TODO It is hard to understand which actions are handled here. It is also error prone as the order might change.
					git.getItem(0).setEnabled(true);
					git.getItem(1).setEnabled(true);
					// disable the diff action if there are 2 or more files selected or if
					// the files selected is a directory
					if (selectedFiles.length > 1 || selectedFiles[0].isDirectory()) {
						JMenuItem item = git.getItem(1);
						item.setEnabled(false);
					} else {
						FileStatus selectedFile = null;
						String previousRepository = OptionsManager.getInstance().getSelectedRepository();
						GitAccess.getInstance().setRepository(repository);
						List<FileStatus> gitFiles = GitAccess.getInstance().getUnstagedFiles();
						gitFiles.addAll(GitAccess.getInstance().getStagedFile());
						String selectedFilePath = selectedFiles[0].getAbsolutePath().replace("\\", "/");
						for (FileStatus fileStatus : gitFiles) {
							if (selectedFilePath.endsWith(fileStatus.getFileLocation())) {
								selectedFile = new FileStatus(fileStatus);
								break;
							}
						}
						if (selectedFile == null 
						    || selectedFile != null && (
						        selectedFile.getChangeType() == GitChangeType.ADD 
						        || selectedFile.getChangeType() == GitChangeType.UNTRACKED
						        || selectedFile.getChangeType() == GitChangeType.MISSING
						        || selectedFile.getChangeType() == GitChangeType.REMOVED)) {
							JMenuItem item = git.getItem(1);
							item.setEnabled(false);
						}
						
						GitAccess.getInstance().setRepository(previousRepository);
					}
					// set the text and make it visible
					git.setVisible(true);
					git.setOpaque(true);
					git.setBackground(Color.WHITE);

					// set icon on MenuItem
					git.setIcon(Icons.getIcon(ImageConstants.GIT_ICON));

					// add a separator
					popupMenu.addSeparator();

					// add menuItem at popupMenu
					popupMenu.add(git);
				}
			}

		} catch (Exception e) {
		}
		return result;
	}
}