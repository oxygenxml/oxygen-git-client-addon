package com.oxygenxml.git.service;

import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.PushPullEvent;

public class TestUtil {
  public static void collectPushPullEvents(GitController pc, final StringBuilder b) {
    pc.addGitListener(new GitEventAdapter() {
      @Override
      public void operationAboutToStart(GitEventInfo changeEvent) {
        if (changeEvent instanceof PushPullEvent) {
          b.append(changeEvent).append("\n");
        }
      }
      @Override
      public void operationSuccessfullyEnded(GitEventInfo changeEvent) {
        if (changeEvent instanceof PushPullEvent) {
          b.append(changeEvent).append("\n");
        }
      }
    });
  }
}
