package com.github.stokito.IdeaJol;

import javax.swing.table.AbstractTableModel;
import java.util.List;

class FieldLayoutTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"Offset", "Size", "Type", "Class", "Field"};
    private final List<Object[]> layouts;

    public FieldLayoutTableModel(List<Object[]> layouts) {
        this.layouts = layouts;
    }

    public int getColumnCount() {
        return COLUMNS.length;
    }

    public int getRowCount() {
        return layouts.size();
    }

    public Object getValueAt(int row, int col) {
        return layouts.get(row)[col];
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
