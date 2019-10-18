package com.oxygenxml.git.service;

import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;

public class RebaseUncommittedChangesException extends GitAPIException {

  private List<String> uncommittedChanges;
  
  public RebaseUncommittedChangesException(List<String> uncommittedChanges) {
    super("");
    this.uncommittedChanges = uncommittedChanges;
  }

  public List<String> getUncommittedChanges() {
    return uncommittedChanges;
  }

}
