package com.oxygenxml.git.utils.script;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests the loading of an update script into memory. 
 */
public class GenerateScriptTest {

  /**
   * Loads a Git repository change script.
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testLoad() throws Exception {
    RepoGenerationScript emps = RepoGenerationScript.loadScript(
        "<script>\n" + 
        "    <changeSet message=\"First commit.\">\n" + 
        "        <change path=\"f1/file1.txt\" type=\"add\">new file content</change>\n" + 
        "        <change path=\"f1/file2.txt\" type=\"add\">new content</change>\n" + 
        "        <change path=\"f2/file1.txt\" type=\"add\"/>\n" + 
        "    </changeSet>\n" + 
        "    \n" + 
        "    <changeSet message=\"Second commit.\">\n" + 
        "        <change path=\"f1/file1.txt\" type=\"change\">file content</change>\n" + 
        "        <change path=\"f1/file2.txt\" type=\"change\">new content</change>\n" + 
        "        <change path=\"f2/file1.txt\" type=\"delete\"/>\n" + 
        "    </changeSet>\n" + 
        "</script>\n" + 
        "");
    
    StringBuilder b = new StringBuilder();
    emps.changeSet.forEach(c -> dump(c, b));
    
    assertEquals(
        "First commit.\n" + 
        "f1/file1.txt - add - new file content - \n" + 
        "f1/file2.txt - add - new content - \n" + 
        "f2/file1.txt - add -  - \n" + 
        "Second commit.\n" + 
        "f1/file1.txt - change - file content - \n" + 
        "f1/file2.txt - change - new content - \n" + 
        "f2/file1.txt - delete -  - \n" + 
        "", b.toString());
     
  }

  private static void dump(Change c, StringBuilder b) {
    b.append(c.path).append(" - ");
    b.append(c.type).append(" - ");
    b.append(c.content).append(" - ");
    b.append("\n");
  }

  static void dump(ChangeSet c, StringBuilder b) {
    b.append(c.message).append("\n");
    
    c.changes.forEach(ch -> dump(ch, b));
  }

}
