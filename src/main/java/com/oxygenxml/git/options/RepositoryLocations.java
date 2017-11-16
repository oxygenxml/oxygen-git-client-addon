package com.oxygenxml.git.options;

import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
	private LinkedList<String> locations = new LinkedList<String>();

	public LinkedList<String> getLocations() {
		return locations;
	}

	public void setLocations(LinkedList<String> locations) {
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
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RepositoryLocations other = (RepositoryLocations) obj;
		if (locations == null) {
			if (other.locations != null)
				return false;
		} else if (!locations.equals(other.locations))
			return false;
		return true;
	}

}
