package com.oxygenxml.git.service;

import org.eclipse.jgit.transport.RemoteRefUpdate.Status;

public class PushResponse {
	private Status status;
	private String message;

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
