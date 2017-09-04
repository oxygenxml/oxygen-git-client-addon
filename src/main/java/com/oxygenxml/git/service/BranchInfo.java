package com.oxygenxml.git.service;

public class BranchInfo {

	private String branchName;
	private String shortBranchName;
	private boolean isDetached;

	public BranchInfo() {

	}

	public BranchInfo(String branchName, boolean isDetached) {
		this.branchName = branchName;
		this.isDetached = isDetached;

	}

	public String getBranchName() {
		return branchName;
	}

	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

	public boolean isDetached() {
		return isDetached;
	}

	public void setDetached(boolean isDetached) {
		this.isDetached = isDetached;
	}

	public String getShortBranchName(){
		return shortBranchName;
	}
	
	public void setShortBranchName(String shortBranchName) {
		this.shortBranchName = shortBranchName;
	}

}
