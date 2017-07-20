package com.oxygenxml.sdksamples.workspace.git.view;

import javax.swing.JOptionPane;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.sdksamples.workspace.git.service.GitAccess;
import com.oxygenxml.sdksamples.workspace.git.utils.UserCredentials;

public class AppWorker implements Runnable{

	private UserCredentials userCredentials;
	private GitAccess gitAccess;
	private boolean push;
	
	public AppWorker(UserCredentials userCredentials, GitAccess gitAccess, boolean push){
		this.userCredentials = userCredentials;
		this.gitAccess = gitAccess;
		this.push = push;
	}
	
	@Override
	public void run() {
		if(push){
			try {
				gitAccess.push(userCredentials.getUsername(), userCredentials.getPassword());
				JOptionPane.showMessageDialog(null, "Push successful");
			} catch (GitAPIException e1) {
				JOptionPane.showMessageDialog(null, "Invalid credentials");
				e1.printStackTrace();
			}
		} else {
			gitAccess.pull(userCredentials.getUsername(), userCredentials.getPassword());
		}
	}

}
