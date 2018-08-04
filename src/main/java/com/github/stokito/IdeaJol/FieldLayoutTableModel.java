package com.github.stokito.IdeaJol;

import javax.swing.table.AbstractTableModel;

class FieldLayoutTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"Offset", "Size", "Type", "Class", "Field"};
    private final Object[][] layouts;

    public FieldLayoutTableModel(Object[][] layouts) {
        this.layouts = layouts;
    }

    public int getColumnCount() {
        return COLUMNS.length;
    }

    public int getRowCount() {
        return layouts.length;
    }

    public Object getValueAt(int row, int col) {
        return layouts[row][col];
    }

    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    public Class getColumnClass(int c) {
        if (c == 0) {
            return Integer.class;
        }
        if (c == 1) {
            return Integer.class;
        }
        return Object.class;
    }


}
