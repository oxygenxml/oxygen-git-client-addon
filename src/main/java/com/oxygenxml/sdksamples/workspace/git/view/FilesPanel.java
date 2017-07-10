package com.oxygenxml.sdksamples.workspace.git.view;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class FilesPanel extends JPanel {

	private List<FilePanel> filePanels = new ArrayList<FilePanel>();
	private List<String> fileNames = new ArrayList<String>();

	public FilesPanel() {
		init();
	}

	private void init() {
		this.setBorder(BorderFactory.createTitledBorder("Files Panel"));
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		
		for (String fileName : fileNames) {
			FilePanel filePanel = new FilePanel(fileName);
			filePanels.add(filePanel);
			this.add(filePanel);
		}
		
		this.revalidate();
		this.repaint();

	}

	public void addFilePanel(FilePanel filePanel) {
		filePanels.add(filePanel);
		init();
	}

	public List<FilePanel> getFilePanels() {
		return filePanels;
	}

	public void setFilePanels(List<FilePanel> filePanels) {
		this.filePanels = filePanels;
	}

	public List<String> getFileNames() {
		return fileNames;
	}

	public void setFileNames(List<String> fileNames) {
		this.fileNames = fileNames;
		this.removeAll();
		init();
	}

}
