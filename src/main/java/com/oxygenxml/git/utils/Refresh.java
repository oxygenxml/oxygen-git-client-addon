package com.oxygenxml.git.utils;

import javax.swing.JComponent;

import com.oxygenxml.git.view.StagingPanel;

public interface Refresh {
	
	public void call();

	public void setPanel(JComponent stagingPanel);
}
