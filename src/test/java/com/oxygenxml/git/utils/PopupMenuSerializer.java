package com.oxygenxml.git.utils;

import java.awt.Component;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

/**
 * Utility class to serialize a menu.
 */
public class PopupMenuSerializer {
  /**
   * Hidden constructor.
   */
  private PopupMenuSerializer() {
    // Nothing.
  }
  
  /**
   * Serialize popup menu.
   * 
   * @param popUp The popup menu.
   * @param includeEnableStateInfo <code>true</code> to include the enable state 
   * information
   * @param serializeSeparators <code>true</code> to serialize separators.
   * @return The popup menu serialization.
   */
  public static String serializePopupStructure(JPopupMenu popUp, 
      boolean includeEnableStateInfo, boolean serializeSeparators) {
    StringBuilder str = new StringBuilder();
    Component[] components = popUp.getComponents();
    serializeComponentsStructure(str, 0, components, includeEnableStateInfo, 
        false, serializeSeparators);
    return str.toString();
  }

  /**
   * Serialize menu components structure.
   * 
   * @param str The string builder to serialize.
   * @param indents Indents counts.
   * @param components The menu components.
   * @param includeEnableStateInfo <code>true</code> to include the enable state 
   * information
   * @param lastWasSep <code>true</code> if the last serialized component was 
   * a separator.
   * @param serializeSeparators <code>true</code> to serialize separators.
   */
  private static void serializeComponentsStructure(StringBuilder str, int indents, 
      Component[] components, boolean includeEnableStateInfo, boolean lastWasSep, boolean serializeSeparators) {
    for (Component comp : components) {
      if (!(comp instanceof JSeparator)) {
        str.append("\n");
        for (int i = 0; i < indents; i++) {
          str.append("  ");
        }
        if (comp instanceof JMenuItem) {
          str.append(((JMenuItem)comp).getText());
          if (includeEnableStateInfo) {
            str.append(" [" + (comp.isEnabled() ? "ENABLED" : "DISABLED") + "]");
          }
          lastWasSep = false;
          if (comp instanceof JMenu) {
            serializeComponentsStructure(str, indents + 1, 
                ((JMenu) comp).getMenuComponents(), 
                  includeEnableStateInfo, lastWasSep, serializeSeparators);
          }
        } else {
          str.append(comp);
        }
      } else if (serializeSeparators){
        if (!lastWasSep) {
          str.append("\n");
          for (int i = 0; i < indents; i++) {
            str.append("  ");
          }
          str.append("----");
        }
        lastWasSep = true;
      }
    }
  }

}
