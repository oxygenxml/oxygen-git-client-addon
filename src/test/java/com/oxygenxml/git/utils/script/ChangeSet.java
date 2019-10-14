package com.oxygenxml.git.utils.script;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A set of changes to be performed on a Git repository.
 */
@XmlRootElement(name = "changeSet")
public class ChangeSet {
  /**
   * Commit message.
   */
  @XmlAttribute(name = "message")
  String message;
  /**
   * Current branch. Short name.
   */
  @XmlAttribute(name = "branch")
  String branch;
  /**
   * Short name of a branch to merge into the current one.
   */
  @XmlAttribute(name = "mergeBranch")
  String mergeBranch;
  /**
   * A set of file changes.
   */
  @XmlElement(name = "change")
  ArrayList<Change> changes;
}
