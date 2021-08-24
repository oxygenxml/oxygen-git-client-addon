package com.oxygenxml.git.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.oxygenxml.git.service.TestUtil;

import javassist.Modifier;

/**
 * @author Alex_Smarandache
 */
public class TagsTest {

  
  /**
   * Check if exists unused tags.
   * 
   * @author Alex_Smarandache
   * 
   * @throws Exception
   */
  @Test
  public void testAllTagsUsed() throws Exception {
    List allTags = new ArrayList(500);
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
        if ("src".equals(file.getName()) || file.getName().startsWith("module-")) {
          check(file, allTags);
        }
      }
    }
    assertEquals("All tags should be used:", "[]", allTags.toString());
  }

  
  /**
   * Check if the file has the Java type and remove the contained files from the tag list .
   * 
   * @param file
   * @param tags
   * @throws IOException
   */
  private void check(File file, List tags) throws IOException {
    if (tags.size() > 0) {
      if (file.isFile() && file.getName().endsWith("java")) {
        String content = TestUtil.read(file.toURI().toURL());
        for (int i = tags.size() - 1; i >= 0; i--) {
          String tag = (String) tags.get(i);
          if (content.indexOf(tag) > 0) {
            tags.remove(i);
          }
        }
      } else if (file.isDirectory()) {
        File[] children = file.listFiles();
        for (int i = 0; i < children.length; i++) {
          check(children[i], tags);
        }
      }
    }
  }

  
  /**
   * Check if into translation.xml are duplicate keys.
   * 
   * @author radu_coravu
   * @throws Exception
   */
  @Test
  public void testDuplicateKeys() throws Exception {
    checkDuplicatedKeys("i18n/translation.xml");
  }

  
  /**
   * Check if there are duplicated keys in some translation file
   * 
   * @param translationFilePath The path of translation file.
   * @throws SAXException
   * @throws IOException
   * @throws ParserConfigurationException
   */
  private void checkDuplicatedKeys(String translationFilePath)
      throws SAXException, IOException, ParserConfigurationException {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(translationFilePath);
    NodeList keys = doc.getDocumentElement().getElementsByTagName("key");
    List duplicateKeys = new ArrayList();
    List keyList = new ArrayList();
    int length = keys.getLength();
    for (int i = 0; i < length; i++) {
      Element keyElem = (Element) keys.item(i);
      String keyVal = keyElem.getAttribute("value");
      if (keyVal.length() == 0) {
        assertTrue("Empty key into translation.xml", false);
      }
      if (keyList.contains(keyVal)) {
        duplicateKeys.add(keyVal);
      } else {
        keyList.add(keyVal);
      }
    }
    if (duplicateKeys.size() > 0) {
      StringBuilder duplicatedKeys = new StringBuilder();
      for (Object key : duplicateKeys) {
        duplicatedKeys.append("\n" + key);
      }
      System.err.println("Duplicate keys : " + duplicatedKeys.toString());
      assertEquals("Should be no duplicate keys into translation", "[]", duplicateKeys.toString());
    }
  }

  
  /**
   * Check if into translation.xml, all keys are a correspondent to Tags.
   * 
   * @author Alex_Smarandache
   * 
   * @throws Exception
   */
  @Test
  public void testIfAllTagsHaveAMatch() throws Exception {
    checkCorrespondentKeys("i18n/translation.xml");
  }

  
  /**
   * Check if into translation.xml, all keys are a correspondent to Tags.
   * 
   * @author Alex_Smarandache
   * 
   * @param translationFilePath The path of translation file.
   * @throws SAXException
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   */
  private void checkCorrespondentKeys(String translationFilePath)
      throws SAXException, IOException, ParserConfigurationException, IllegalArgumentException, IllegalAccessException {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(translationFilePath);
    NodeList keys = doc.getDocumentElement().getElementsByTagName("key");
    String[] oxygenKeys = {"Filter_hint", "Close", "Cancel", "Preferences", "Git_client", "Create"}; 
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
    if (unresolvedTags.size() > 0) {
      StringBuilder missedKeys = new StringBuilder();
      for (Object key : unresolvedTags) {
        missedKeys.append("\n" + key);
      }
      System.err.println("Missed keys : " + missedKeys.toString());
      assertEquals("All tags must have a correspondent key in translation.xml", "[]", unresolvedTags.toString());
    }
  }

}
