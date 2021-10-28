package com.oxygenxml.git.view.history.graph;

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
	private final transient GraphRender cellRender = new GraphRender();
	
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
	
	
	@Override
	public Component getTableCellRendererComponent(JTable arg0, Object arg1, boolean arg2, boolean arg3, int arg4,
			int arg5) {
		this.table = arg0;
		this.value = arg1;

		return this;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	protected void paintComponent(Graphics g) {
		if(shouldBePainted) {
			cellRender.paint((PlotCommit<VisualCommitsList.VisualLane>)value, 
					table.getRowHeight(), 
					(Graphics2D)g);
		}
	}
	
    /**
     * @param shouldBePainted <code>true</code> if the cell should be painted.
     */
	public void setShouldBePainted(boolean shouldBePainted) {
		this.shouldBePainted = shouldBePainted;
	}

}
