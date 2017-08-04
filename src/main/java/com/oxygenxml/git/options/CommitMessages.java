package com.oxygenxml.git.options;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Entity for the JAXB to store the list of commit messages. Stores last 7
 * committed messages
 * 
 * @author Beniamin Savu
 *
 */
@XmlRootElement(name = "commitMessages")
@XmlAccessorType(XmlAccessType.FIELD)
public class CommitMessages {

	/**
	 * The last 7 committed messages
	 */
	@XmlElement(name = "message")
	private List<String> messages = new ArrayList<String>();

	public List<String> getMessages() {
		return messages;
	}

	public void setMessages(List<String> messages) {
		this.messages = messages;
	}

}