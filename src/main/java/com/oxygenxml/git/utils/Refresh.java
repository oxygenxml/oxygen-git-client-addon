package com.oxygenxml.git.utils;

import javax.swing.JComponent;

public interface Refresh {
	
	public void call();

	public void setPanel(JComponent stagingPanel);
}
