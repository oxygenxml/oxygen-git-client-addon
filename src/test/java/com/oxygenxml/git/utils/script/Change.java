package com.oxygenxml.git.utils.script;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

/**
 * A changed to perform on a file.
 */
@XmlRootElement(name = "change")
public class Change {
  /**
   * Type of change: add, change, delete
   */
  @XmlAttribute(name = "type")
  String type;
  /**
   * path relative to the working tree directory.
   */
  @XmlAttribute(name = "path")
  String path;
  /**
   * New file content.
   */
  @XmlValue
  String content;
  
  /**
   * @return The change type as an {@link GitChangeType} value.
   */
  private GitChangeType getChangeType() {
    if ("add".equals(type)) {
      return GitChangeType.UNTRACKED;
    } else if ("change".equals(type)) {
      return GitChangeType.MODIFIED;
    } else if ("delete".equals(type)) {
      return GitChangeType.REMOVED;
    }
    
    return GitChangeType.UNTRACKED;
  }
  
  /**
   * @return An identical file change, as a {@link FileStatus}
   */
  FileStatus toFileStatus() {
    return new FileStatus(getChangeType(), path);
  }
}
