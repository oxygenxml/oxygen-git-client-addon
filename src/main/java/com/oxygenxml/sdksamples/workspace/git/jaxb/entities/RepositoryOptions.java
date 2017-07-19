package com.oxygenxml.sdksamples.workspace.git.jaxb.entities;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Entity for the JAXB to store a list of elements
 * 
 * @author intern2
 *
 */
@XmlRootElement(name = "repositoryOptions")
@XmlAccessorType(XmlAccessType.FIELD)
public class RepositoryOptions {

	@XmlElement(name = "option")
	private Set<RepositoryOption> repositoryOptions = new HashSet<RepositoryOption>();

	public Set<RepositoryOption> getRepositoryOptions() {
		return repositoryOptions;
	}

	public void setRepositoryOptions(Set<RepositoryOption> repositoryOptions) {
		this.repositoryOptions = repositoryOptions;
	}
	

}
