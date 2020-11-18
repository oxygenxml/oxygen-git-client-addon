package com.oxygenxml.git.service;

import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.PushPullEvent;

public class TestUtil {
  public static void collectPushPullEvents(GitController pc, final StringBuilder b) {
    pc.addGitListener(new GitEventAdapter() {
      @Override
      public void operationAboutToStart(GitEventInfo changeEvent) {
        if (changeEvent instanceof PushPullEvent) {
          b.append("Status: STARTED, message: ").append(((PushPullEvent) changeEvent).getMessage()).append("\n");
        }
      }
      @Override
      public void operationSuccessfullyEnded(GitEventInfo changeEvent) {
        if (changeEvent instanceof PushPullEvent) {
          b.append("Status: FINISHED, message: ").append(((PushPullEvent) changeEvent).getMessage()).append("\n");
        }
      }
      @Override
      public void operationFailed(GitEventInfo info, Throwable t) {
        if (info instanceof PushPullEvent) {
          b.append("Status: FAILED, message: ").append(((PushPullEvent) info).getMessage()).append("\n");
        }
      }
    });
  }
}
