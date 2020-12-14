package com.oxygenxml.git.utils;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * Equaler.
 */
public class Equaler {

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(Equaler.class.getName());

  /**
   * Hidden constructor.
   */
  private Equaler() {
    // Nada
  }

  /**
   * Verify if the two Collections of Objects are equal, including the cases
   * when they are null.
   * 
   * @param o1
   *          First object collection to be compared.
   * @param o2
   *          Second object collection to be compared.
   * @return True if objects are equal or both null, false otherwise.
   */
  @SuppressWarnings("java:S3776")
  public static boolean verifyListEquals(Collection<?> o1, Collection<?> o2) {
    if (o1 == o2) {
      return true;
    } else if (o1 == null || o2 == null) {
      return false;
    } else if (o1.size() != o2.size()) {
      return false;
    } else {
      Iterator<?> itero1 = o1.iterator();
      Iterator<?> itero2 = o2.iterator();
      while (itero1.hasNext()) {
        Object o1Obj = itero1.next();
        Object o2Obj = itero2.next();
        if (o1Obj instanceof Collection && o2Obj instanceof Collection) {
          if (!verifyListEquals((Collection<?>) o1Obj, (Collection<?>) o2Obj)) {
            return false;
          }
        } else {
          if (!verifyEquals(o1Obj, o2Obj)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * Verify if the two objects are equal, including the cases when they are
   * null. If the objects are not null and are URL instances the equals
   * verification is done by avoiding resolving the hosts of the URL because
   * that is a long operation.
   * 
   * @param o1
   *          First object to be compared.
   * @param o2
   *          Second object to be compared.
   * @return True if objects are equal or both null, false otherwise.
   */
  public static boolean verifyEquals(Object o1, Object o2) {
    boolean equals = false;
    if (o1 == o2) {
      // The same instance.
      equals = true;
    } else if (o1 == null) {
      equals = false;
    } else if (o2 == null) {
      equals = false;
    } else if (o1 instanceof CharSequence) {
      equals = verifyStringObjectEquals(o1, o2);
    } else {
      if (o1 instanceof URL) {
        o1 = ((URL) o1).toExternalForm();
      }
      if (o2 instanceof URL) {
        o2 = ((URL) o2).toExternalForm();
      }
      if (LOGGER.isDebugEnabled()) {
        checkObjectsForArrays(o1, o2);
      }
      equals = o1.equals(o2);
    }
    return equals;
  }

  @SuppressWarnings("java:S3776")
  private static boolean verifyStringObjectEquals(Object first, Object second) {
    if (first instanceof CharSequence && second instanceof CharSequence) {
      if (first instanceof String && second instanceof String) {
        return first.equals(second);
      } else {
        CharSequence firstString = (CharSequence) first;
        CharSequence secondString = (CharSequence) second;
        int n = firstString.length();
        if (n == secondString.length()) {
          for (int i = 0; i < n; i++) {
            if (firstString.charAt(i) != secondString.charAt(i)) {
              return false;
            }
          }
          return true;
        } else {
          return false;
        }
      }
    } else {
      if (LOGGER.isDebugEnabled()) {
        checkObjectsForArrays(first, second);
      }
      return first.equals(second);
    }
  }

  /**
   * Check if the given objects are arrays.
   * 
   * @param o1
   *          The object to check if is an array.
   * @param o2
   *          The object to check if is an array.
   */
  protected static void checkObjectsForArrays(Object o1, Object o2) {
    // It may happen for someone to compare an array with an object which is not
    // an array
    boolean firstIsArray = o1.getClass().isArray();
    boolean secondIsArray = o2.getClass().isArray();
    if (firstIsArray != secondIsArray) {
      LOGGER.debug("Comparing an array with a non array object", new Exception());
    }
  }

}
