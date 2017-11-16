package com.oxygenxml.git.options;

import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Entity for the JAXB to store the list of destination paths.
 * 
 * @author Beniamin Savu
 *
 */
@XmlRootElement(name = "destinationPaths")
@XmlAccessorType(XmlAccessType.FIELD)
public class DestinationPaths {

	/**
	 * The list with the previous destination paths
	 */
	@XmlElement(name = "path")
	private LinkedList<String> paths = new LinkedList<String>();

	public LinkedList<String> getPaths() {
		return paths;
	}

	public void setPaths(LinkedList<String> paths) {
		this.paths = paths;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((paths == null) ? 0 : paths.hashCode());
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
		DestinationPaths other = (DestinationPaths) obj;
		if (paths == null) {
			if (other.paths != null)
				return false;
		} else if (!paths.equals(other.paths))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DestinationPaths [paths=" + paths + "]";
	}

}
