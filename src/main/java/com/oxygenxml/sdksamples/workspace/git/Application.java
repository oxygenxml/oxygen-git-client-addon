package com.oxygenxml.sdksamples.workspace.git;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TreeFormatter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevCommitList;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;

import com.oxygenxml.sdksamples.workspace.git.service.GitAccess;
import com.oxygenxml.sdksamples.workspace.git.service.entities.UnstageFile;
import com.oxygenxml.sdksamples.workspace.git.view.CommitPanel;
import com.oxygenxml.sdksamples.workspace.git.view.GitWindow;
import com.oxygenxml.sdksamples.workspace.git.view.StagedChangesPanel;
import com.oxygenxml.sdksamples.workspace.git.view.StagingPanel;
import com.oxygenxml.sdksamples.workspace.git.view.UnstagedChangesPanel;
import com.oxygenxml.sdksamples.workspace.git.view.WorkingCopySelectionPanel;

public class Application {

	private final static String LOCAL_TEST_REPOSITPRY = "src/main/resources";

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
		WorkingCopySelectionPanel workingCopySelectionPanel = new WorkingCopySelectionPanel(gitAccess);
		UnstagedChangesPanel unstagedChangesPanel = new UnstagedChangesPanel(gitAccess);
		StagedChangesPanel stagedChangesPanel = new StagedChangesPanel(gitAccess);
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
