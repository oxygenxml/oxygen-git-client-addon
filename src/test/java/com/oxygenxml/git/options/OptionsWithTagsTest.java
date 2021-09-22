package com.oxygenxml.git.options;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Used for testing the methods in OptionsWithTags
 * 
 * @author gabriel_nedianu
 *
 */
public class OptionsWithTagsTest {

  @Test
  public void testArrayToMapAndViceVersa() {
    
    Map<String, String> map = new HashMap<>();
    map.put("k1", "v1");
    map.put("k2", "v2");
    map.put("k3", "v3");
    String[] arrayFromMap = OptionsWithTags.mapToArray(map);
    
    List<String> list = Arrays.asList(arrayFromMap);
    assertEquals("[k1, v1, k2, v2, k3, v3]", list.toString());
    
    Map<String, String> mapFromArray = OptionsWithTags.arrayToMap(arrayFromMap);
    assertEquals("{k1=v1, k2=v2, k3=v3}", mapFromArray.toString());
  }

}
