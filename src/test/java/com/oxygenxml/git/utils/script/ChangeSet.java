package com.oxygenxml.git.utils.script;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "changeSet")
public class ChangeSet {

  @XmlAttribute(name = "message")
  String message;
  
  @XmlAttribute(name = "branch")
  String branch;
  
  @XmlAttribute(name = "mergeBranch")
  String mergeBranch;
  
  @XmlElement(name = "change")
  ArrayList<Change> changes;
}
