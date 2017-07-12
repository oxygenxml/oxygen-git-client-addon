package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.oxygenxml.sdksamples.workspace.git.constants.Constants;

public class TableRendererEditor implements TableCellEditor, TableCellRenderer {

	private JTable table;
	private JButton button;
	private int[] hovered = null;
	private int[] previousHovered = null;

	private int[] clicked = null;

	private int column = 0;
	private int row = 0;

	public TableRendererEditor(JTable table) {

		this.table = table;
		this.button = new JButton();

		addMouseMotionListener();
		addMouseListener();
	}

	private void addMouseListener() {
		table.addMouseListener(new MouseListener() {

			public void mouseReleased(MouseEvent e) {
				int row = clicked[0];
				int col = clicked[1];
				clicked = null;
				table.repaint(table.getCellRect(row, col, true));

			}

			public void mousePressed(MouseEvent e) {

				Point point = new Point(e.getX(), e.getY());

				row = table.convertRowIndexToModel(table.rowAtPoint(point));
				column = table.columnAtPoint(point);
				if (column == 2) {

					TableModel model = table.getModel();
					Class<?> columnClass = model.getColumnClass(column);

					if (String.class.equals(columnClass) && column == 2 && point.getX() < table.getWidth() - 10
							&& point.getY() > 2 && row != -1) {
						clicked = new int[] { row, column };
					}
					table.repaint(table.getCellRect(clicked[0], clicked[1], true));
				}

			}

			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			public void mouseClicked(MouseEvent e) {

			}

		});

	}

	public void render() {
		TableColumn column = table.getColumnModel().getColumn(Constants.STAGE_BUTTON_COLUMN);
		column.setCellRenderer(this);
		column.setCellEditor(this);
	}

	private void addMouseMotionListener() {
		table.addMouseMotionListener(new MouseMotionListener() {

			public void mouseMoved(MouseEvent e) {
				Point point = new Point(e.getX(), e.getY());
				row = table.convertRowIndexToModel(table.rowAtPoint(point));
				column = table.columnAtPoint(point);

				TableModel model = table.getModel();
				Class<?> columnClass = model.getColumnClass(column);

				if (hovered != null) {
					previousHovered = new int[] { hovered[0], hovered[1] };
				}

				if (String.class.equals(columnClass) && column == 2 && point.getX() < table.getWidth() - 10 && point.getY() > 2
						&& row != -1) {
					hovered = new int[] { row, column };
				} else {
					hovered = null;
				}

				if (hovered != null) {
					table.repaint(table.getCellRect(hovered[0], hovered[1], true));
					if (previousHovered != null) {
						table.repaint(table.getCellRect(previousHovered[0], previousHovered[1], true));
					}
				} else if (previousHovered != null) {
					table.repaint(table.getCellRect(previousHovered[0], previousHovered[1], true));
					previousHovered = null;
				}
			}

			public void mouseDragged(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});

	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

		return null;
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

		boolean click = clicked != null && clicked[0] == row && clicked[1] == column;
		button.getModel().setSelected(click);

		return button;

	}

}
