package com.oxygenxml.git.view.history;

import java.util.Comparator;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.view.stash.FilesTableModel;

/**
 * Model for affected files in History.
 * 
 * @author Alex_Smarandache
 *
 */
public class HistoryTableAffectedFilesModel extends FilesTableModel {

  /**
   * Path for the presented file in history.
   */
  private String presentedFile = null;
  
  /**
   * Compares file statuses.
   */
  private final transient Comparator<FileStatus> defaultComparator = (f1, f2) -> {
    int comparationResult = 0;
    
    if(presentedFile != null && presentedFile.length() > 0) {
      boolean file1IsFiltered = !presentedFile.equals(f1.getFileLocation()) &&
          !f1.getFileLocation().startsWith(presentedFile + "/", 0);
      boolean file2IsFiltered = !presentedFile.equals(f2.getFileLocation()) &&
          !f2.getFileLocation().startsWith(presentedFile + "/", 0);
      comparationResult = Boolean.compare(file1IsFiltered, file2IsFiltered);
    }
    
    if(comparationResult == 0) {
      // Both are filtered or both are matched. Second level sort.
      comparationResult = f1.getChangeType().compareTo(f2.getChangeType());
      if(comparationResult == 0) {
        // Same change type. Third level sort.
        comparationResult = f1.getFileLocation().compareTo(f2.getFileLocation());
      }
    }
    
    return comparationResult;
  };
  
  
  /**
   * Constructor.
   */
  public HistoryTableAffectedFilesModel() {
    setComparator(defaultComparator);
  }

 
  /**
   * Sets the new presented file. If no file is presented then set null value. 
   * 
   * @param presentedFile The new String path of presented file.
   */
  public void setPresentedFile(String presentedFile) {
    this.presentedFile = presentedFile;
  }
  
  
}
