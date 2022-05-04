package com.oxygenxml.git.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.annotations.NonNull;

import ro.sync.document.DocumentPositionedInfo;

/**
 * A collector for project problems.
 * 
 * @author alex_smarandache
 *
 */
public class ProblemsCollector implements ICollector {

  /**
   * All current problems collected.
   */
  private final List<DocumentPositionedInfo> problems = new ArrayList<>();
  
  @Override
  public void add(@NonNull final DocumentPositionedInfo[] dpis) {
    problems.addAll(Arrays.asList(dpis));
  }

  @Override
  public @NonNull DocumentPositionedInfo[] getAll() {
    final DocumentPositionedInfo[] dpis = new DocumentPositionedInfo[problems.size()];
    for(int i = 0; i < dpis.length; i++) {
      dpis[i] = problems.get(i);
    }

    return dpis;
  }

  @Override
  public void reset() {
    problems.clear(); 
  }

  @Override
  public boolean isEmpty() {
    return problems.isEmpty();
  }

}
