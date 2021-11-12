package com.oxygenxml.git.view.history.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.oxygenxml.git.view.dialog.CheckoutCommitDialog;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * 
 * 
 * @author alex_smarandache
 *
 */
public class CheckoutCommitAction extends AbstractAction {
    // TODO complete this class
	public CheckoutCommitAction() {
		super("Checkout...");
	}
	@Override
	public void actionPerformed(ActionEvent e) {
	    OKCancelDialog dialog = new CheckoutCommitDialog();
	    System.out.println(dialog.getResult());
		
	}

	
}
