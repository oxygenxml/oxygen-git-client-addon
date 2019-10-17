package com.oxygenxml.git.service.entities;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

/**
 * A wrapper over a diff entry representing a changed suffered by a resource. 
 */
public class FileStatusOverDiffEntry extends FileStatus {
  /**
   * Diff entry representing a changed suffered by a resource.
   */
  private DiffEntry entry;

  public FileStatusOverDiffEntry(DiffEntry entry) {
    super(map(entry), getPath(entry));
    
    this.entry = entry;
  }

  /**
   * @param entry Diff entry representing a changed suffered by a resource.
   * 
   * @return
   */
  private static String getPath(DiffEntry entry) {
    if (ChangeType.DELETE == entry.getChangeType()) {
      return entry.getOldPath();
    }
    
    return entry.getNewPath();
  }
  
  
  private static GitChangeType map(DiffEntry entry) {
    GitChangeType toreturn = GitChangeType.ADD;
    ChangeType diffChange = entry.getChangeType();
    if (ChangeType.ADD == diffChange) {
      
    } else if (ChangeType.COPY == diffChange) {
      
    } else if (ChangeType.DELETE == diffChange) {
      toreturn = GitChangeType.REMOVED;
    } else if (ChangeType.MODIFY == diffChange) {
      toreturn = GitChangeType.CHANGED;
    }
    
    return toreturn;
  }
  
  public DiffEntry getDiffEntry() {
    return entry;
  }

}
