package com.oxygenxml.git.view.staging;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;

/**
 * Test cases.
 */
public class FlatView7Test extends FlatViewTestBase {

	@Override
	public void setUp() throws Exception {
		super.setUp();

		stagingPanel.getUnstagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
		stagingPanel.getStagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
	}


	/**
	 * <p><b>Description:</b> Test the tooltips of the pull/push buttons and branch label.</p>
	 * <p><b>Bug ID:</b> EXM-45599, EXM-44564</p>
	 *
	 * @author sorin_carbunaru
	 * @author Alex_Smarandache
	 * 
	 * @throws Exception
	 */
	@Test
	public void testToolbarComponentsTooltips() throws Exception {
		// Set toolbar panel
		stagingPanel.setToolbarPanelFromTests(
				new ToolbarPanel(
						(GitController) stagingPanel.getGitController(),
						gitActionsManager));

		// Create repositories
		String localTestRepository = "target/test-resources/test_EXM_45599_local";
		String localTestRepository_2 = "target/test-resources/test_EXM_45599_local_2";
		String remoteTestRepository = "target/test-resources/test_EXM_45599_remote";
		Repository remoteRepo = createRepository(remoteTestRepository);
		Repository localRepo = createRepository(localTestRepository);
		bindLocalToRemote(localRepo , remoteRepo);
		Repository localRepo_2 = createRepository(localTestRepository_2);
		bindLocalToRemote(localRepo_2 , remoteRepo);

		pushOneFileToRemote(localTestRepository, "init.txt", "hello");
		flushAWT();
		
		BranchSelectionCombo branchesCombo = stagingPanel.getBranchesCombo();
		branchesCombo.refresh();
		flushAWT();
		
		// Create local branch
		Git git = GitAccess.getInstance().getGit();
		git.branchCreate().setName("new_branch").call();
		GitAccess.getInstance().setBranch("new_branch");
		

		ToolbarPanel toolbarPanel = stagingPanel.getToolbarPanel();
		branchesCombo.refresh();
    flushAWT();
    refreshSupport.call();
    flushAWT();

		assertEquals(
				"<html>Cannot_pull<br>No_remote_branch.</html>",
				toolbarPanel.getPullMenuButton().getToolTipText());
		assertEquals(
				"<html>Push_to_create_and_track_remote_branch</html>",
				toolbarPanel.getPushButton().getToolTipText());
		assertTrue(toolbarPanel.isBranchNotPublished());
		assertEquals(
				"<html>Local_branch <b>new_branch</b>.<br>Upstream_branch <b>No_upstream_branch</b>.<br></html>",
				branchesCombo.getToolTipText());

		// Push to create the remote branch
		((GitController) stagingPanel.getGitController()).push();
		waitForScheduler();
		
		branchesCombo.refresh();
		flushAWT();
		refreshSupport.call();
		flushAWT();

		// Tooltip texts changed
		assertEquals(
				"<html>Pull_merge_from.<br>Toolbar_Panel_Information_Status_Up_To_Date<br><br></html>",
				toolbarPanel.getPullMenuButton().getToolTipText());
		assertEquals(
				"<html>Push_to.<br>Nothing_to_push<br><br></html>",
				toolbarPanel.getPushButton().getToolTipText());
		assertEquals(
				"<html>Local_branch <b>new_branch</b>.<br>Upstream_branch <b>origin/new_branch</b>.<br>"
						+ "Toolbar_Panel_Information_Status_Up_To_Date<br>Nothing_to_push</html>",
						branchesCombo.getToolTipText());

		GitAccess.getInstance().setBranch(GitAccess.DEFAULT_BRANCH_NAME);
		flushAWT();

		// Commit a new file locally
		commitOneFile(localTestRepository, "anotherFile.txt", "");
		waitForScheduler();

		// Commit to remote
		commitOneFile(remoteTestRepository, "anotherFile_2.txt", "");
		waitForScheduler();

		GitAccess.getInstance().setRepositorySynchronously(localTestRepository);
		flushAWT();
		GitAccess.getInstance().fetch();
		branchesCombo.refresh();
		refreshSupport.call();
		flushAWT();
		sleep(1000);
		
		// Tooltip texts changed again
		String expected = "<html>Pull_merge_from.<br>One_commit_behind<br><br>&#x25AA; Date, Hour &ndash; AlexJitianu (2 files)"
				+ "<br>&nbsp;&nbsp;&nbsp;New file: anotherFile_2.txt<br></html>";
		String regexDate = "(([0-9])|([0-2][0-9])|([3][0-1]))\\ (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\ \\d{4}";
		String regexHour = "(\\d\\d:\\d\\d)";
		String actual = toolbarPanel.getPullMenuButton().getToolTipText();
		actual = actual.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour");
		assertEquals(expected, actual); 

		expected = "<html>Push_to.<br>One_commit_ahead<br><br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile.txt<br></html>";
		actual = toolbarPanel.getPushButton().getToolTipText();
		actual = actual.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour");
		assertEquals(expected, actual);  

		actual = branchesCombo.getToolTipText();
		expected = "<html>Local_branch <b>" + GitAccess.DEFAULT_BRANCH_NAME + "</b>.<br>Upstream_branch <b>origin/" + GitAccess.DEFAULT_BRANCH_NAME + "</b>.<br>"
    		+ "One_commit_behind<br>One_commit_ahead</html>";
    assertEquals(expected, actual);

		// Commit a new change locally
		commitOneFile(localTestRepository, "anotherFile.txt", "changed");
		waitForScheduler();

		// Commit to remote
		commitOneFile(remoteTestRepository, "anotherFile_2.txt", "changed");
		waitForScheduler();

		GitAccess.getInstance().setRepositorySynchronously(localTestRepository);
		GitAccess.getInstance().fetch();
		branchesCombo.refresh();
		flushAWT();
    refreshSupport.call();
    flushAWT();

		expected =  "<html>Pull_merge_from.<br>Commits_behind<br><br>&#x25AA; Date, Hour &ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile_2.txt<br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (2 files)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile_2.txt<br></html>";
		actual = toolbarPanel.getPullMenuButton().getToolTipText();
		assertEquals(
				expected,
				actual.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour")
				); 

		expected = "<html>Push_to.<br>Commits_ahead<br><br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile.txt<br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile.txt<br></html>";
		actual = toolbarPanel.getPushButton().getToolTipText();
		assertEquals(
				expected,
				actual.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour")
				);  

		assertEquals(
				"<html>Local_branch <b>" + GitAccess.DEFAULT_BRANCH_NAME + "</b>.<br>Upstream_branch <b>origin/" + GitAccess.DEFAULT_BRANCH_NAME + "</b>.<br>"
						+ "Commits_behind<br>Commits_ahead</html>",
						branchesCombo.getToolTipText());

		// Commit a new change locally
		commitOneFile(localTestRepository, "anotherFile200000000000000000000000000000000000000000000000000000000000"
				+ "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
				+ "000000000000000000000000000000000000000000000000000"
				+ ".txt", "changed2");
		waitForScheduler();

		// Commit to remote
		commitOneFile(remoteTestRepository, "anotherFile300000000000000000000000000000000000000000000000000000000000"
				+ "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
				+ "000000000000000000000000000000000000000000000000000"
				+ ".txt", "changed3");
		waitForScheduler();

		GitAccess.getInstance().setRepositorySynchronously(localTestRepository);
		GitAccess.getInstance().fetch();
		branchesCombo.refresh();
		flushAWT();
    refreshSupport.call();
		flushAWT();

		expected =  "<html>Pull_merge_from.<br>Commits_behind<br><br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile300000000000000000000000000000000000000...<br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile_2.txt<br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (2 files)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile_2.txt<br></html>";
		actual = toolbarPanel.getPullMenuButton().getToolTipText();
		assertEquals(
				expected,
				actual.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour")
				);  

		expected = "<html>Push_to.<br>Commits_ahead<br><br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile200000000000000000000000000000000000000...<br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile.txt<br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile.txt<br></html>";
		actual = toolbarPanel.getPushButton().getToolTipText();
		assertEquals(
				expected,
				actual.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour")
				);  

		String[] filesForCommit = {
				"6anotherFile45.txt",
				"5anotherFile45.txt",
				"4anotherFile45.txt",
				"3anotherFile45.txt",
				"2anotherFile45.txt",
				"1anotherFile45.txt",
				"anotherFil233e45.txt",
				"anotherFil333e45.txt",
				"anotherFileee45.txt",
				"anotherFile45w.txt"
		};

		for (int i = 0; i < filesForCommit.length; i++) {
			// Commit a new change locally
			commitOneFile(localTestRepository, filesForCommit[i], "changed");
			waitForScheduler();

			// Commit to remote
			commitOneFile(remoteTestRepository, "_" + filesForCommit[i], "changed");
			waitForScheduler();
		}

		GitAccess.getInstance().setRepositorySynchronously(localTestRepository);
		GitAccess.getInstance().fetch();
		branchesCombo.refresh();
    flushAWT();
    refreshSupport.call();
    flushAWT();
    sleep(1000);

		expected =  "<html>Pull_merge_from.<br>Commits_behind<br><br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: _anotherFile45w.txt<br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: _anotherFileee45.txt<br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: _anotherFil333e45.txt<br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: _anotherFil233e45.txt<br>&#x25AA; [...] "
				+ "&ndash; N_More_Commits<br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (2 files)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile_2.txt<br><br>See_all_commits_in_Git_History</html>";
		actual = toolbarPanel.getPullMenuButton().getToolTipText();
		assertEquals(
				expected,
				actual.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour")
				);  

		expected = "<html>Push_to.<br>Commits_ahead<br><br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile45w.txt<br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFileee45.txt<br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFil333e45.txt<br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFil233e45.txt<br>&#x25AA; [...] "
				+ "&ndash; N_More_Commits<br>&#x25AA; Date, Hour "
				+ "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile.txt<br><br>See_all_commits_in_Git_History</html>";
		actual = toolbarPanel.getPushButton().getToolTipText();
		assertEquals(
				expected.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour"),
				actual.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour")
				);      
	}
	

}
