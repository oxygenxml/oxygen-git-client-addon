package com.oxygenxml.git.view.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.historycomponents.CommitCharacteristics;
import com.oxygenxml.git.view.historycomponents.HistoryCommitTableModel;
import com.oxygenxml.git.view.historycomponents.HistoryHyperlinkListener;
import com.oxygenxml.git.view.historycomponents.HistoryTableRenderer;
import com.oxygenxml.git.view.historycomponents.RowHistoryTableSelectionListener;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Dialog showing the commit history.
 * 
 * @Alexandra_Dinisor
 *
 */
public class HistoryDialog extends OKCancelDialog {

	private static final String SHOWING_HISTORY_FOR = "Showing history for: ";

	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(HistoryDialog.class);

	/**
	 * Plugin workspace access.
	 */
	private PluginWorkspace pluginWorkspace;

	/**
	 * Git access.
	 */
	private GitAccess gitAccess = GitAccess.getInstance();

	/**
	 * Table for showing commit history.
	 */
	JTable historyTable;

	/**
	 * The translator for the messages that are displayed in this dialog
	 */
	private static Translator translator = Translator.getInstance();

	public HistoryDialog() throws MissingObjectException, IncorrectObjectTypeException, NoHeadException, IOException, GitAPIException {
		super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
				translator.getTranslation(Tags.GIT_COMMIT_HISTORY), false);

		getOkButton().setVisible(false);
		getCancelButton().setVisible(false);

		this.pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();

		this.setMinimumSize(new Dimension(1475, 260));
		this.setResizable(true);

		createGUI();

		this.pack();
		this.setLocationRelativeTo((JFrame) pluginWorkspace.getParentFrame());
		this.setVisible(true);
	}

	/**
	 * Create the components for the history dialog.
	 * 
	 * @throws GitAPIException
	 * @throws IOException
	 * @throws NoHeadException
	 * @throws IncorrectObjectTypeException
	 * @throws MissingObjectException
	 */
	private void createGUI()
			throws MissingObjectException, IncorrectObjectTypeException, NoHeadException, IOException, GitAPIException {
		try {
			Container contentPane = this.getContentPane();
			contentPane.setLayout(new BorderLayout());

			JLabel showCurrentRepoLabel = new JLabel(SHOWING_HISTORY_FOR + gitAccess.getRepository().getDirectory().toString());
			showCurrentRepoLabel.setBorder(BorderFactory.createEmptyBorder(0,2,5,0));

			try {
				Class tableClass = Class.forName("ro.sync.exml.workspace.api.standalone.ui.Table");
				Constructor tableConstructor = tableClass.getConstructor();
				historyTable = (JTable) tableConstructor.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
				historyTable = new JTable();
			}

			Vector<CommitCharacteristics> commitCharacteristicsVector = gitAccess.getCommitsCharacteristics();

			historyTable.setModel(new HistoryCommitTableModel(commitCharacteristicsVector));
			historyTable.setDefaultRenderer(CommitCharacteristics.class, new HistoryTableRenderer(gitAccess, gitAccess.getRepository()));
			JScrollPane tableScrollPane = new JScrollPane(historyTable);
			historyTable.setFillsViewportHeight(true);

			// set Commit Description Pane with HTML content and hyperlink.
			JEditorPane commitDescriptionPane = new JEditorPane();
			commitDescriptionPane.setContentType("text/html");

			historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			historyTable.getSelectionModel().addListSelectionListener(
					new RowHistoryTableSelectionListener(historyTable, commitDescriptionPane, commitCharacteristicsVector));

			commitDescriptionPane
					.addHyperlinkListener(new HistoryHyperlinkListener(historyTable, commitCharacteristicsVector));
			commitDescriptionPane.setEditable(false);

			// preliminary select the first row in the historyTable
			historyTable.setRowSelectionInterval(0, 0);

			// set minimum width for commit message column
			historyTable.getColumnModel().getColumn(0).setMinWidth(400);

			JScrollPane commitDescriptionScrollPane = new JScrollPane(commitDescriptionPane);
			JScrollPane fileHierarchyScrollPane = new JScrollPane(new JEditorPane());

			Dimension minimumSize = new Dimension(500, 150);
			commitDescriptionScrollPane.setPreferredSize(minimumSize);
			fileHierarchyScrollPane.setPreferredSize(minimumSize);

			JSplitPane infoBoxesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, commitDescriptionScrollPane,
					fileHierarchyScrollPane);
			infoBoxesSplitPane.setDividerLocation(0.5);
			infoBoxesSplitPane.setOneTouchExpandable(true);

			contentPane.add(showCurrentRepoLabel, BorderLayout.NORTH);

			JSplitPane centerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, infoBoxesSplitPane);
			centerSplitPane.setDividerLocation(0.7);

			contentPane.add(centerSplitPane, BorderLayout.CENTER);

		} catch (NoRepositorySelected e) {
			logger.debug(e, e);
			e.printStackTrace();
		}

	}	

}
