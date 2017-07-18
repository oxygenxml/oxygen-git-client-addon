package com.oxygenxml.sdksamples.workspace.git;

import java.io.IOException;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TreeFormatter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;

import com.oxygenxml.sdksamples.workspace.git.service.GitAccess;
import com.oxygenxml.sdksamples.workspace.git.view.CommitPanel;
import com.oxygenxml.sdksamples.workspace.git.view.GitWindow;
import com.oxygenxml.sdksamples.workspace.git.view.StagedChangesPanel;
import com.oxygenxml.sdksamples.workspace.git.view.StagingPanel;
import com.oxygenxml.sdksamples.workspace.git.view.UnstagedChangesPanel;
import com.oxygenxml.sdksamples.workspace.git.view.WorkingCopySelectionPanel;
import com.oxygenxml.sdksamples.workspace.git.view.event.Observer;
import com.oxygenxml.sdksamples.workspace.git.view.event.StageController;

public class Application {

	private final static String LOCAL_TEST_REPOSITPRY = "src/main/resources";

	// C:/Users/intern2/Documents/test
	// C:/Users/intern2/Documents/Oxygen-Git-Plugin
	
	public static void main(String[] args) {
		new Application().start();
		
	}

	private void start() {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		createAndShowFrame();

	}

	private void createAndShowFrame() {
		GitAccess gitAccess = new GitAccess();
		StageController observer = new StageController(gitAccess);
		
		UnstagedChangesPanel unstagedChangesPanel = new UnstagedChangesPanel(gitAccess, observer, false);
		UnstagedChangesPanel stagedChangesPanel = new UnstagedChangesPanel(gitAccess, observer, true);
		WorkingCopySelectionPanel workingCopySelectionPanel = new WorkingCopySelectionPanel(gitAccess);
		//StagedChangesPanel stagedChangesPanel = new StagedChangesPanel(gitAccess, observer);
		CommitPanel commitPanel = new CommitPanel(gitAccess);

		StagingPanel stagingPanel = new StagingPanel(workingCopySelectionPanel, unstagedChangesPanel, stagedChangesPanel,
				commitPanel);

		GitWindow gitWindow = new GitWindow(stagingPanel);
		gitWindow.createGUI();
	}

	private static void test(Git git) {
		try {
			// add data to a file
			Repository repository = git.getRepository();
			ObjectInserter objectInserter = repository.newObjectInserter();
			byte[] bytes = "Hello World!".getBytes("utf-8");
			ObjectId blobId = objectInserter.insert(Constants.OBJ_BLOB, bytes);
			objectInserter.flush();

			// read the data from the file
			ObjectReader objectReader = repository.newObjectReader();
			ObjectLoader objectLoader = objectReader.open(blobId);
			int type = objectLoader.getType();
			// Constants.OBJ_BLOB
			bytes = objectLoader.getBytes();
			String helloWorld = new String(bytes, "utf-8");
			// Hello World!
			System.out.println(helloWorld);

			// add the data grom above to a specific file
			TreeFormatter treeFormatter = new TreeFormatter();
			treeFormatter.append("hello-world.java", FileMode.REGULAR_FILE, blobId);
			ObjectId treeId = objectInserter.insert(treeFormatter);
			objectInserter.flush();

			// get the file name
			TreeWalk treeWalk = new TreeWalk(repository);
			treeWalk.addTree(treeId);
			treeWalk.next();
			String filename = treeWalk.getPathString(); // hello-world.txt
			System.out.println(filename);

			// create a commit
			CommitBuilder commitBuilder = new CommitBuilder();
			commitBuilder.setTreeId(treeId);
			commitBuilder.setMessage("My first commit!");
			PersonIdent person = new PersonIdent("me", "me@example.com");
			commitBuilder.setAuthor(person);
			commitBuilder.setCommitter(person);
			objectInserter = repository.newObjectInserter();
			ObjectId commitId = objectInserter.insert(commitBuilder);
			objectInserter.flush();

			objectReader = repository.newObjectReader();
			objectLoader = objectReader.open(commitId);
			RevCommit commit = RevCommit.parse(objectLoader.getBytes());
			System.out.println(commit);

		} catch (IOException e) { // TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
