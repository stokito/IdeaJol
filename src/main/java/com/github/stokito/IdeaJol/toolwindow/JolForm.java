package com.github.stokito.IdeaJol.toolwindow;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;

class JolForm {
    JComponent rootPanel;
    JPanel pnlToolbar;
    JLabel lblClassName;
    JButton copyButton;
    JPanel pnlLayouter;
    JComboBox<String> cmbDataModel;
    JPanel pnlInstanceSize;
    JLabel lblInstanceSize;
    JLabel lblLossesInternal;
    JLabel lblLossesExternal;
    JLabel lblLossesTotal;
    JPanel pnlObjectLayout;
    JBScrollPane objectLayoutScrollPane;
    JBTable tblObjectLayout;
}
