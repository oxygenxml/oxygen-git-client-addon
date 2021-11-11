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
   * Presenter for file in history.
   */
  private transient FileHistoryPresenter fileHistoryPresenter;
  
 
  
  /**
   * Sets the new presented file. If no file is presented then set null value. 
   * 
   * @param presentedFile The new String path of presented file.
   */
  public void setFilePathPresenter(FileHistoryPresenter presentedFile) {
    this.fileHistoryPresenter = presentedFile;
    Comparator<FileStatus> comparator = (f1, f2) -> {
        int comparationResult = 0;
        String searchedFile = fileHistoryPresenter.getFilePath();
        if(searchedFile != null && searchedFile.length() > 0) {
          boolean file1IsFiltered = !searchedFile.equals(f1.getFileLocation()) &&
              !f1.getFileLocation().startsWith(searchedFile + "/", 0);
          boolean file2IsFiltered = !searchedFile.equals(f2.getFileLocation()) &&
              !f2.getFileLocation().startsWith(searchedFile + "/", 0);
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
      
    setComparator(comparator);
  }


  /**
   * @return The actual FilePresenter.
   */
  public FileHistoryPresenter getFilePathPresenter() {
    return fileHistoryPresenter;
  }
  
  
}
