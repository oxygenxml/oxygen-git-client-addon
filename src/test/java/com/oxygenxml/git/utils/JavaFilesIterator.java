/*
 * Copyright (c) 2018 Syncro Soft SRL - All Rights Reserved.
 *
 * This file contains proprietary and confidential source code.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */
package com.oxygenxml.git.utils;

import java.io.File;
import java.io.FileFilter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Iterates recursively over the files from the given folder, in level order, returning the 
 * collected files as URLs.
 * 
 * @author octavian_nadolu
 */
public class JavaFilesIterator implements Iterator<File> {
  /**
   * The current directories queue to be processed.
   */
  private Queue<File> dirsQueue = new LinkedList<File>();

  /**
   * The buffered files queue to return files from.
   */
  private Queue<File> filesQueue = new LinkedList<File>();

  /**
   * The filter, used to filter the returned files.
   */
  private final FileFilter javaFileFilter = new FileFilter() {
    @Override
    public boolean accept(File file) {
      boolean accept = file.isDirectory();
      if (!accept) {
        accept = acceptFile(file);
      }
      
      return accept;
    }
  };

  /**
   * Constructor.
   * 
   * @param startFile       The file to start the collecting from. 
   */
  public JavaFilesIterator(File startFile) {
    if (startFile.isDirectory()) {
      // Add the folder in the dirsQueue, to be processed recursively.
      dirsQueue.add(startFile);
    } else if (javaFileFilter.accept(startFile)) {
      // Add the current file in the files to return queue, if is not filtered.
      filesQueue.add(startFile);
    }
  }
  
  /**
   * @see java.util.Iterator#next()
   */
  @Override
  public File next() {
    if (filesQueue.isEmpty()) {
      // Fill the next files queue.
      collectNextFiles();
    }
    // Get the next file.
    File file = null;
    if (!filesQueue.isEmpty()) {
      file = filesQueue.remove();
    }
    return file;
  }

  /**
   * @see java.util.Iterator#hasNext()
   */
  @Override
  public boolean hasNext() {
    if (filesQueue.isEmpty()) {
      // Fill the next files queue.
      collectNextFiles();
    }
    return !filesQueue.isEmpty();
  }

  /**
   * @see java.util.Iterator#remove()
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException("Cannot remove elements from this iterator.");
  }

  /**
   * Collect the next files, and add them in the files queue.
   */
  private void collectNextFiles() {
    while (filesQueue.isEmpty() && !dirsQueue.isEmpty()) {
      // Pull the current directory from the queue.
      File currentDir = dirsQueue.remove();
      // Get the files from the current folder.
      File[] children = currentDir.listFiles();
      if (children != null) {
        for (File child : children) {
          // Check if the file is filtered.
          if (javaFileFilter.accept(child)) {
            if (child.isFile()) {
              // The child is a file, add it to the files queue.
              filesQueue.add(child);
            } else {
              // The child is a folder.
              // Must be processed later recursively.
              dirsQueue.add(child);
            }
          }
        }
      }
    }
  }
  
  /**
   * Returns <code>true</code> if the given filename is accepted.
   * 
   * @param file The file.
   * 
   * @return <code>true</code> if the given filename is accepted.
   */
  protected boolean acceptFile(File file) {
    String filename = null;
    // Get the filename
    if (file != null) {
      filename = file.getName();
    }
    
   return filename != null &&
       filename.endsWith(".java") && 
       !filename.endsWith("Test.java") &&
       !filename.endsWith("TestBase.java") &&
       !filename.endsWith("TestCase.java"); 
  }
}