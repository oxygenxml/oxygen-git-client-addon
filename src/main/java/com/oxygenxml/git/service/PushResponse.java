package com.oxygenxml.git.service;

import org.eclipse.jgit.transport.RemoteRefUpdate.Status;

/**
 * After a push is completed this is the response it will generate. The response
 * will have a status and a message to display
 * 
 * @author Beniamin Savu
 *
 */
public class PushResponse {
	/**
	 * The push status after it finishes
	 */
	private Status status;

	/**
	 * The message that will be displayed
	 */
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

	@Override
	public String toString() {
	  return "status: " + status + " message " + message;
	}
}
