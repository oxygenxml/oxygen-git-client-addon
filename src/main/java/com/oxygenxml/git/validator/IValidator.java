package com.oxygenxml.git.validator;

import java.util.List;

import com.oxygenxml.git.service.entities.FileStatus;

/**
 * Interface used to validate a list of @FileStatus. 
 * 
 * @author alex_smarandache
 *
 */
public interface IValidator {
  
  /**
   * Validate the given files.
   * 
   * @param files The files to be validated.
   */
  public void validate(final List<FileStatus> files);
  
}
