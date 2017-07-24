package com.oxygenxml.git.jaxb.entities;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Entity for the JAXB to store the list of repository locations
 * 
 * @author intern2
 *
 */
@XmlRootElement(name = "repositoryLocations")
@XmlAccessorType(XmlAccessType.FIELD)
public class RepositoryLocations {

	@XmlElement(name = "location")
	private Set<String> locations = new HashSet<String>();

	public Set<String> getLocations() {
		return locations;
	}

	public void setLocations(Set<String> locations) {
		this.locations = locations;
	}
	

}
