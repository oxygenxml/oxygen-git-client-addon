package com.oxygenxml.git.options;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import ro.sync.util.Equaler;

/**
 * Entity for the JAXB to store the list of repository locations
 * 
 * @author Beniamin Savu
 *
 */
@XmlRootElement(name = "repositoryLocations")
@XmlAccessorType(XmlAccessType.FIELD)
public class RepositoryLocations {

	/**
	 * The list with the users repositories
	 */
	@XmlElement(name = "location")
	private List<String> locations = new LinkedList<String>();

	public List<String> getLocations() {
		return locations;
	}

	public void setLocations(List<String> locations) {
		this.locations = locations;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((locations == null) ? 0 : locations.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
	  boolean toReturn = false;
	  if (obj instanceof RepositoryLocations) {
	    RepositoryLocations locs = (RepositoryLocations) obj;
	    toReturn = Equaler.verifyListEquals(locations, locs.getLocations());
	  }
	  return toReturn;
	}

}
