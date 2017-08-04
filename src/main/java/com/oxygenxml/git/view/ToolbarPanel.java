package com.oxygenxml.git.view;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.view.event.Command;
import com.oxygenxml.git.view.event.PushPullController;

/**
 * Contains additional support buttons like push and pull
 * 
 * @author Beniamin Savu
 *
 */
public class ToolbarPanel extends JPanel {

	private JToolBar gitToolbar;
	private PushPullController pushPullController;
	private JButton pushButton;
	private JButton pullButton;
	private JButton storeCredentials;
	private int pushesAhead = 0;
	private int pullsBehind = 0;

	public ToolbarPanel(PushPullController pushPullController) {
		this.pushPullController = pushPullController;
	}

	public JButton getPushButton() {
		return pushButton;
	}

	public JButton getPullButton() {
		return pullButton;
	}

	public void setPullsBehind(int pullsBehind) {
		this.pullsBehind = pullsBehind;
		pullButton.repaint();
	}

	public void setPushesAhead(int pushesAhead) {
		this.pushesAhead = pushesAhead;
		pushButton.repaint();
	}

	/**
	 * Sets the panel layout and creates all the buttons with their functionality
	 * making the visible
	 */
	public void createGUI() {
		this.setLayout(new BorderLayout());
		this.pushesAhead = GitAccess.getInstance().getPushesAhead();
		this.pullsBehind = GitAccess.getInstance().getPullsBehind();
		addPushAndPullButtons();
		// addUndefinedButton();

		this.add(gitToolbar, BorderLayout.PAGE_START);
	}

	/**
	 * Adds to the tool bar the Push and Pull Buttons
	 */
	private void addPushAndPullButtons() {
		gitToolbar = new JToolBar();
		gitToolbar.setFloatable(false);
		pushButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(ImageConstants.GIT_PUSH_ICON))) {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				// TODO Paint the counter
				String str = "";
				if (pushesAhead > 0) {
					str = "" + pushesAhead;
				}
				g.setFont(g.getFont().deriveFont(Font.BOLD));
				FontMetrics fontMetrics = g.getFontMetrics(g.getFont());
				int stringWidth = fontMetrics.stringWidth(str);
				g.drawString(str, pushButton.getWidth() - stringWidth, pushButton.getHeight() - fontMetrics.getDescent());
			}
		};
		pushButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				System.out.println("Conflict before push " + GitAccess.getInstance().getConflict());
				pushPullController.execute(Command.PUSH);
				System.out.println("Conflict after push " + GitAccess.getInstance().getConflict());
				if (pullsBehind == 0) {
					pushesAhead = 0;
				}
			}
		});
		pushButton.setToolTipText("Push");
		gitToolbar.add(pushButton);

		pullButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(ImageConstants.GIT_PULL_ICON))) {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				String str = "";
				if (pullsBehind > 0) {
					str = "" + pullsBehind;
				}
				g.setFont(g.getFont().deriveFont(Font.BOLD));
				FontMetrics fontMetrics = g.getFontMetrics(g.getFont());
				int stringWidth = fontMetrics.stringWidth(str);
				g.drawString(str, pushButton.getWidth() - stringWidth, pushButton.getHeight() - fontMetrics.getDescent());
			}

		};
		pullButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				pushPullController.execute(Command.PULL);
				pullsBehind = 0;
			}
		});
		pullButton.setToolTipText("Pull");
		gitToolbar.add(pullButton);

	}

	/**
	 * Remains to be done
	 */
	private void addUndefinedButton() {
		storeCredentials = new JButton(
				new ImageIcon(getClass().getClassLoader().getResource(ImageConstants.STORE_CREDENTIALS_ICON)));
		storeCredentials.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
			}

		});
		storeCredentials.setToolTipText("Update Credentials");
		gitToolbar.add(storeCredentials);
	}

}
