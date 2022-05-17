package com.oxygenxml.git.view.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Contains methods for collections.
 * 
 * @author alex_smarandache
 *
 */
public class CollectionsUtil {

  /**
   * Hidden constructor.
   */
  private CollectionsUtil() {
    // not needed
  }
  
  /**
   * Convert an iterator to a list.
   * 
   * @param iterator The iterator to be converted.
   * 
   * @return The new list or an empty list.
   */
  public static <T> List<T> toList(Iterator<T> iterator) {
    final List<T> list = new ArrayList<>();
    while(iterator.hasNext()) {
      list.add(iterator.next());
    }
    
    return list;
  }
}
