package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

public class TableButton implements TableCellEditor, TableCellRenderer {

	private JTable table;
	private JButton button;
	private JLabel label;
	private int[] hovered = null;

	private int column = 0;

	public TableButton(JTable table) {
		this.table = table;
		this.button = new JButton();
		this.label = new JLabel();
		addMouseMotionListener();
	}

	private void addMouseMotionListener() {
		table.addMouseMotionListener(new MouseMotionListener() {

			public void mouseMoved(MouseEvent e) {
				Point point = new Point(e.getX(), e.getY());
				int row = table.convertRowIndexToModel(table.rowAtPoint(point));
				int previousColumn = column;
				column = table.columnAtPoint(point);

				TableModel model = table.getModel();
				Class<?> columnClass = model.getColumnClass(column);
				if (String.class.equals(columnClass) && column == 2) {
					// System.out.println("row " + row + " col " + column);
					hovered = new int[] { row, column };
				} else {
					hovered = null;
				}

				// TODO repaint only if necessary.
				// TODO Repaint just the rectangle of interest.
				if (column - previousColumn == 1 && column == 2) {
					table.repaint(table.getCellRect(row, column, true));
				}
				// table.repaint();

			}

			public void mouseDragged(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});

	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

		if (value == null) {
			button.setText("");
			button.setIcon(null);
		} else {
			button.setText(value.toString());
			button.setIcon(null);
		}
		System.out.println("normal");
		return button;
	}

	public Object getCellEditorValue() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isCellEditable(EventObject anEvent) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean shouldSelectCell(EventObject anEvent) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean stopCellEditing() {
		// TODO Auto-generated method stub
		return false;
	}

	public void cancelCellEditing() {
		// TODO Auto-generated method stub

	}

	public void addCellEditorListener(CellEditorListener l) {
		// TODO Auto-generated method stub

	}

	public void removeCellEditorListener(CellEditorListener l) {
		// TODO Auto-generated method stub

	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		if (isSelected) {
			button.setForeground(table.getSelectionForeground());
			button.setBackground(table.getSelectionBackground());
		} else {
			button.setForeground(table.getForeground());
			button.setBackground(UIManager.getColor("Button.background"));
		}

		if (value == null) {
			button.setText("");
			button.setIcon(null);
		} else {
			button.setText(value.toString());
			button.setIcon(null);
		}

		boolean hov = hovered != null && hovered[0] == row && hovered[1] == column;
		button.getModel().setRollover(hov);

		return button;

	}

}
