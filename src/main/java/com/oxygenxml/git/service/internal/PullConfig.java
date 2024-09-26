package com.oxygenxml.git.service.internal;

import java.util.Optional;

import com.oxygenxml.git.view.event.PullType;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PullConfig {
  
  /**
   * The name of the branch from where to pull.
   */
  @Builder.Default
  private Optional<String> branchName = Optional.empty();

  /**
   * The remote from where to do the pull.
   */
  @Builder.Default
  private Optional<String> remote = Optional.empty();

  /**
   * One of ff, no-ff, ff-only, rebase.
   */
  @Builder.Default
  private PullType pullType = PullType.MERGE_FF;

  /**
   * <code>true</code> to execute the equivalent of a "git submodule update --recursive".
   */
  @Builder.Default
  private boolean updateSubmodule = false;
  
}
