package com.oxygenxml.git.utils.script;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

@XmlRootElement(name = "change")
public class Change {
  
  @XmlAttribute(name = "type")
  String type;
  
  @XmlAttribute(name = "path")
  String path;
  
  @XmlValue
  String content;
  
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
  
  FileStatus toFileStatus() {
    return new FileStatus(getChangeType(), path);
  }
}
