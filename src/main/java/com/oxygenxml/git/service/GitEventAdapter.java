package com.oxygenxml.git.service;

import com.oxygenxml.git.view.event.GitEventInfo;

/**
 * An adapter.
 */
public class GitEventAdapter implements GitEventListener {
  @Override
  public void operationAboutToStart(GitEventInfo info) {
    // Auto-generated method stub
  }

  @Override
  public void operationSuccessfullyEnded(GitEventInfo info) {
    // Auto-generated method stub
  }

  @Override
  public void operationFailed(GitEventInfo info, Throwable t) {
    // Auto-generated method stub
  }
}
