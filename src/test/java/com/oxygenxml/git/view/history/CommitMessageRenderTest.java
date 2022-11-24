package com.oxygenxml.git.view.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;

import org.junit.Test;

/**
 * Tests the commit message render from Git History.
 * 
 * @author alex_smarandache
 *
 */
public class CommitMessageRenderTest {
  
  /**
   * Tests if the labels are shortest well when space is or not enough.
   */
  @Test
  public void testShortenCommitLabels() {
    final List<JLabel> commitLabels = new ArrayList<>();
    
    final JLabel l1 = new JLabel();
    l1.setText("abcdefghijklmnopqrstuwxyz");
    l1.setToolTipText("abcdefghijklmnopqrstuwxyz");
    commitLabels.add(l1);
    
    final JLabel l2 = new JLabel();
    l2.setText("123456");
    l2.setToolTipText("123456");
    commitLabels.add(l2);
    
    final JLabel l3 = new JLabel();
    l3.setText("12345678901234567890123456789012345678901234567");
    l3.setToolTipText("12345678901234567890123456789012345678901234567");
    commitLabels.add(l3);
    
    int commitLabelsMaxWidth = (l1.getPreferredSize().width + l2.getPreferredSize().width + l3.getPreferredSize().width) / 3;
    
    CommitMessageTableRenderer.processingCommitLabelsToFitByWidth(commitLabels, commitLabelsMaxWidth);
    
    assertTrue(commitLabels.stream().mapToInt(l -> l.getPreferredSize().width).sum() <= commitLabelsMaxWidth);
    assertTrue(commitLabels.get(0).getToolTipText().length() > commitLabels.get(0).getText().length());
    assertEquals(commitLabels.get(1).getToolTipText().length(), commitLabels.get(1).getText().length());
    assertTrue(commitLabels.get(2).getToolTipText().length() > commitLabels.get(2).getText().length());
    
    commitLabelsMaxWidth *= 5;
    CommitMessageTableRenderer.processingCommitLabelsToFitByWidth(commitLabels, commitLabelsMaxWidth);
    assertEquals(commitLabels.get(0).getToolTipText(), commitLabels.get(0).getText());
    assertEquals(commitLabels.get(1).getToolTipText(), commitLabels.get(1).getText());
    assertEquals(commitLabels.get(2).getToolTipText(), commitLabels.get(2).getText());
  }

}
