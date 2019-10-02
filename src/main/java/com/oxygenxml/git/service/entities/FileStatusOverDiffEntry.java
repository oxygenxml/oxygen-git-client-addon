package com.oxygenxml.git.service.entities;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

public class FileStatusOverDiffEntry extends FileStatus {

  private DiffEntry entry;

  public FileStatusOverDiffEntry(DiffEntry entry) {
    super(map(entry), entry.getNewPath());
    
    this.entry = entry;
  }
  
  
  private static GitChangeType map(DiffEntry entry) {
    // TODO Map
    GitChangeType toreturn = GitChangeType.ADD;
    ChangeType diffChange = entry.getChangeType();
    if (ChangeType.ADD == diffChange) {
      
    } else if (ChangeType.COPY == diffChange) {
      
    } else if (ChangeType.DELETE == diffChange) {
      
    } else if (ChangeType.MODIFY == diffChange) {
      
    } else if (ChangeType.RENAME == diffChange) {
      
    }
    
    return toreturn;
  }

}
