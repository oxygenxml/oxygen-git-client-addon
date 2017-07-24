package com.oxygenxml.git;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class SplitSample extends JFrame {
  protected JSplitPane split;

  public SplitSample() {
    super("Simple Split Pane");
    setSize(400, 400);
    getContentPane().setLayout(new BorderLayout());

    JSplitPane spLeft = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JPanel(), new JPanel());
    spLeft.setDividerSize(8);
    spLeft.setContinuousLayout(true);

    JSplitPane spRight = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JPanel(), new JPanel());
    spRight.setDividerSize(8);
    spRight.setContinuousLayout(true);

    split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, spLeft, spRight);
    split.setContinuousLayout(false);
    split.setOneTouchExpandable(true);

    getContentPane().add(split, BorderLayout.CENTER);

    WindowListener wndCloser = new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    };
    addWindowListener(wndCloser);

    setVisible(true);
  }

  public static void main(String argv[]) {
  	try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
    new SplitSample();
  }
}
    