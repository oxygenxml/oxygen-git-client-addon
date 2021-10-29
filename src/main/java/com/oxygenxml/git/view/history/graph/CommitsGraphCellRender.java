package com.oxygenxml.git.view.history.graph;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.eclipse.jgit.revplot.PlotCommit;

/**
 * 
 * A cell render for history commits graph.
 * 
 * @author alex_smarandache
 *
 */
@SuppressWarnings("serial")
public class CommitsGraphCellRender extends JPanel implements TableCellRenderer {

	/**
	 * The graph render. 
	 */
	private final transient GraphRender cellRender;
	
	/**
	 * The table.
	 */
	private JTable table;
	
	/**
	 * The cell value.
	 */
	private transient Object value;

	/**
	 * <code>true</code> if the cell should be painted.
	 */
	private boolean shouldBePainted = true;
	
	
	
	/**
	 * Constructor.
	 */
	public CommitsGraphCellRender() {
		cellRender = new GraphRender();
	}
	
	
	/**
	 * Constructor.
	 * 
	 * @param Color for dot commit.
	 */
	public CommitsGraphCellRender(Color dotCommitColor) {
		cellRender = new GraphRender(dotCommitColor);
	}
	
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, 
			boolean isSelected, boolean hasFocus, int row, int column) {
		this.table = table;
		this.value = value;
		
		setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

		return this;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	protected void paintComponent(Graphics g) {
		if(shouldBePainted && value != null) {
			Graphics2D g2d = (Graphics2D)g;
			GraphColorUtil.BACKGROUND = getBackground();
			g2d.setBackground(getBackground());
			super.paintComponent(g);
			cellRender.paint((PlotCommit<VisualCommitsList.VisualLane>)value, 
					table.getRowHeight(), 
					g2d
					);
		}
	}
	
	
    /**
     * @param shouldBePainted <code>true</code> if the cell should be painted.
     */
	public void setShouldBePainted(boolean shouldBePainted) {
		this.shouldBePainted = shouldBePainted;
	}

}
