package com.oxygenxml.git.service.exceptions;

import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;

public class RebaseUncommittedChangesException extends GitAPIException {

  private final List<String> uncommittedChanges;
  
  public RebaseUncommittedChangesException(List<String> uncommittedChanges) {
    super("");
    this.uncommittedChanges = uncommittedChanges;
  }

  public List<String> getUncommittedChanges() {
    return uncommittedChanges;
  }

}
