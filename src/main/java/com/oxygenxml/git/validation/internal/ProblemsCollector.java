package com.oxygenxml.git.validation.internal;

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
  
  /**
   * Used to filter problems.
   */
  private IProblemFilter filter;
  
  @Override
  public void add(@NonNull final DocumentPositionedInfo[] dpis) {
    if(filter != null) {
      Arrays.asList(dpis).stream()
      .filter(dpi -> filter.include(dpi))
      .forEach(problems::add);
    } else {
      Arrays.stream(dpis).forEach(problems::add);
    }  
  }

  @Override
  public @NonNull DocumentPositionedInfo[] getAll() {
    return problems.toArray(new DocumentPositionedInfo[problems.size()]);
  }

  @Override
  public void reset() {
    problems.clear(); 
  }

  @Override
  public boolean isEmpty() {
    return problems.isEmpty();
  }

  @Override
  public void setFilter(final IProblemFilter filter) {
    this.filter = filter;
  }
}
