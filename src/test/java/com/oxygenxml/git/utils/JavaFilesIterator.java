/*
 *  The Syncro Soft SRL License
 *
 *  Copyright (c) 1998-2011 Syncro Soft SRL, Romania.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistribution of source or in binary form is allowed only with
 *  the prior written permission of Syncro Soft SRL.
 *
 *  2. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  3. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  4. The end-user documentation included with the redistribution,
 *  if any, must include the following acknowledgment:
 *  "This product includes software developed by the
 *  Syncro Soft SRL (http://www.sync.ro/)."
 *  Alternately, this acknowledgment may appear in the software itself,
 *  if and wherever such third-party acknowledgments normally appear.
 *
 *  5. The names "Oxygen" and "Syncro Soft SRL" must
 *  not be used to endorse or promote products derived from this
 *  software without prior written permission. For written
 *  permission, please contact support@oxygenxml.com.
 *
 *  6. Products derived from this software may not be called "Oxygen",
 *  nor may "Oxygen" appear in their name, without prior written
 *  permission of the Syncro Soft SRL.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE SYNCRO SOFT SRL OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
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