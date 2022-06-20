package com.oxygenxml.git.service.exceptions;

import java.util.List;

import org.eclipse.jgit.api.errors.CheckoutConflictException;

public class RebaseConflictsException extends CheckoutConflictException {

  public RebaseConflictsException(List<String> conflictingPaths) {
    super(conflictingPaths, new org.eclipse.jgit.errors.CheckoutConflictException(""));
  }

}
