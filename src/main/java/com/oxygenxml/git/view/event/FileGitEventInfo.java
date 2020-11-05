package com.oxygenxml.git.view.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

/**
 * Information about a Git event that affected files.
 */
public class FileGitEventInfo extends GitEventInfo {
  
  /**
   * The files affected by the Git event.
   */
  private Collection<String> affectedFiles = Collections.<String> emptyList();

  /**
   * Constructor.
   * 
   * @param gitOp         Git operation.
   * @param affectedFiles Files.
   */
  public FileGitEventInfo(GitOperation gitOp, Collection<String> affectedFiles) {
    super(gitOp);
    this.affectedFiles = new ArrayList<>(affectedFiles);
  }

  /**
   * @return the paths of the files affected by the current event.
   */
  public Collection<String> getAffectedFilePaths() {
    return affectedFiles;
  }

  /**
   * @return the files affected by the current event.
   */
  public List<FileStatus> getAffectedFileStatuses() {
    List<FileStatus> fss = new LinkedList<>();
    for (Iterator<String> iterator = affectedFiles.iterator(); iterator.hasNext();) {
      String path = iterator.next();
      fss.add(new FileStatus(GitChangeType.UNKNOWN, path));
    }
    return fss;
  }
  
  @Override
  public String toString() {
    return "FileGitEventInfo [Operation: " + gitOp + ", affected files: " + affectedFiles + "].";
  }
}
