package com.oxygenxml.git.view.components;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

/**
 * Used to define a section in a preference page for example.
 * 
 * @author alex_smarandache
 *
 */
public class SectionPane extends JPanel {

  /**
   * Default serial version.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   * 
   * @param title The title of the section pane.
   */
  public SectionPane(String title) {
    super(new GridBagLayout());

    final GridBagConstraints constr = new GridBagConstraints();
    // Add the label.
    constr.gridx = 0;
    constr.gridy = 0;
    constr.anchor = GridBagConstraints.WEST;
    constr.insets = new Insets(5, 0, 5, 5);
    final JLabel titleLabel = new JLabel(title);
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
    add(titleLabel, constr);

    // Add the separator.
    constr.gridx++;
    constr.fill = GridBagConstraints.HORIZONTAL;
    constr.anchor = GridBagConstraints.WEST;
    constr.weightx = 1;
    constr.insets.right = 0;
    add(new JSeparator(), constr);
  }
}
