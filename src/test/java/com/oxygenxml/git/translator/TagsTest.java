package com.oxygenxml.git.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.oxygenxml.git.service.TestUtil;

import javassist.Modifier;

/**
 * @author Alex_Smarandache
 */
public class TagsTest {

  /**
   * Check if there are any unused translation tags.
   * 
   * @author Alex_Smarandache
   * 
   * @throws Exception
   */
  @Test
  public void testAllTagsUsed() throws Exception {
    List<String> allTags = new ArrayList<>();
    Field[] fields = Tags.class.getFields();
    for (int i = 0; i < fields.length; i++) {
      if (Modifier.isStatic(fields[i].getModifiers())) {
        String fieldName = fields[i].getName();
        allTags.add("Tags." + fieldName);
      }
    }
    File[] files = new File(".").listFiles();
    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      if (file.isDirectory()) {
        if ("src".equals(file.getName())) {
          removeUsedTags(file, allTags);
        }
      }
    }
    assertEquals("All tags should be used, but found some unused ones:", "[]", allTags.toString());
  }

  
  /**
   * Remove the tags that are used in the current file.
   * 
   * @param file Current file.
   * @param tags All tags.
   * 
   * @throws IOException
   */
  private void removeUsedTags(File file, List<String> tags) throws IOException {
    if (!tags.isEmpty()) {
      if (file.isFile() && file.getName().endsWith("java")) {
        String content = TestUtil.read(file.toURI().toURL());
        for (int i = tags.size() - 1; i >= 0; i--) {
          String tag = tags.get(i);
          if (content.indexOf(tag) != -1) {
            tags.remove(i);
          }
        }
      } else if (file.isDirectory()) {
        File[] children = file.listFiles();
        for (int i = 0; i < children.length; i++) {
          removeUsedTags(children[i], tags);
        }
      }
    }
  }

  
  /**
   * Check if there are duplicate keys in translation.xml.
   * 
   * @throws Exception
   */
  @Test
  public void testDuplicateKeys() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse("i18n/translation.xml");
    NodeList keys = doc.getDocumentElement().getElementsByTagName("key");
    List<String> duplicateKeys = new ArrayList<>();
    List<String> keyList = new ArrayList<>();
    int length = keys.getLength();
    for (int i = 0; i < length; i++) {
      Element keyElem = (Element) keys.item(i);
      String keyVal = keyElem.getAttribute("value");
      if (keyVal.length() == 0) {
        fail("Empty key into translation.xml");
      }
      if (keyList.contains(keyVal)) {
        duplicateKeys.add(keyVal);
      } else {
        keyList.add(keyVal);
      }
    }
    
    StringBuilder duplicatedKeys = new StringBuilder();
    if (!duplicateKeys.isEmpty()) {
      for (Object key : duplicateKeys) {
        duplicatedKeys.append("\n" + key);
      }
    }
    assertEquals("Should be no duplicate keys in translation.xml, but found some:", "[]", duplicateKeys.toString());
  }

  
  /**
   * Check if all tags have corresponding keys in translation.xml.<br><br>
   * OBS: there are some exceptions - the tags defined for the keys loaded from Oxygen's translation.xml.
   * 
   * @author Alex_Smarandache
   * 
   * @throws Exception
   */
  @Test
  public void testIfAllTagsHaveAKey() throws Exception {
    String[] oxygenKeys = {"Filter_hint", "Close", "Cancel", "Preferences", "Git_client", "Create"};
    
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse("i18n/translation.xml");
    NodeList keys = doc.getDocumentElement().getElementsByTagName("key");
    Set<String> existingKeysInOxygen = new HashSet<>(Arrays.asList(oxygenKeys));
    Set<String> keyValues = new HashSet<>();
    List<String> unresolvedTags = new ArrayList<>();
    int length = keys.getLength();
    for (int i = 0; i < length; i++) {
      Element keyElem = (Element) keys.item(i);
      keyValues.add(keyElem.getAttribute("value"));
    }

    Field[] fields = Tags.class.getFields();
    for (int i = 0; i < fields.length; i++) {
      if (Modifier.isStatic(fields[i].getModifiers())) {
        String currentTag = (String)fields[i].get(fields[i]);
        if (!existingKeysInOxygen.contains(currentTag) && !keyValues.contains(currentTag)) {
          unresolvedTags.add((String) fields[i].get(fields[i]));
        }
      }
    }
    
    StringBuilder missedKeys = new StringBuilder();
    if (unresolvedTags.size() > 0) {
      for (Object key : unresolvedTags) {
        missedKeys.append("\n" + key);
      }
    }
    assertEquals("Usually tags should have a correspondent key in translation.xml.", "[]", unresolvedTags.toString());
  
  }

  
  /**
   * Check if all keys have corresponding tag. 
   * @author Alex_Smarandache
   * 
   * @throws Exception
   */
  @Test
  public void testIfAllKeysHaveATag() throws Exception {
  
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse("i18n/translation.xml");
    NodeList keys = doc.getDocumentElement().getElementsByTagName("key");
    Set<String> tagsValues = new HashSet<>();
    List<String> unresolvedKeys = new ArrayList<>();
    
    Field[] fields = Tags.class.getFields();
    for (int i = 0; i < fields.length; i++) {
      if (Modifier.isStatic(fields[i].getModifiers())) {
         tagsValues.add((String) fields[i].get(fields[i]));
      }
    }
    
    int length = keys.getLength();
    for (int i = 0; i < length; i++) {
      Element keyElem = (Element) keys.item(i);
      String key = keyElem.getAttribute("value");
      if(!tagsValues.contains(key)) {
        unresolvedKeys.add(key);
      }
    }

 
    
    StringBuilder missedKeys = new StringBuilder();
    if (unresolvedKeys.size() > 0) {
      for (Object key : unresolvedKeys) {
        missedKeys.append("\n" + key);
      }
    }
    assertEquals("Usually keys should have a correspondent tag.", "[]", unresolvedKeys.toString());
  
  }

  

}
