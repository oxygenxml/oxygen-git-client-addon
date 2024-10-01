package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.history.HistoryController;

/**
 * Action to show history.
 * 
 * @author alex_smarandache
 *
 */
public class ShowHistoryAction extends GitAbstractAction {

	/**
	 * The history controller.
	 */
	private final transient HistoryController historyController;
	
	
	/**
	 * Constructor.
	 * 
	 * @param historyController The history controller.
	 */
	public ShowHistoryAction(HistoryController historyController) {
		super(Translator.getInstance().getTranslation(Tags.SHOW_HISTORY));
		this.historyController = historyController;
		this.putValue(SMALL_ICON, Icons.getIcon(Icons.GIT_HISTORY));
	}
	

	@Override
	public void actionPerformed(ActionEvent e) {
		historyController.showRepositoryHistory();
		
	}
}
