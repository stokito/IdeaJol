package com.github.stokito.IdeaJol.toolwindow;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import java.awt.*;

class JolForm {
    JComponent rootPanel;
    JLabel lblInstanceSize;
    JLabel lblClassName;
    JPanel pnlToolbar;
    JComboBox<String> cmbDataModel;
    JBTable tblObjectLayout;
    JPanel pnlInstanceSize;
    JLabel lblLossesInternal;
    JLabel lblLossesExternal;
    JLabel lblLossesTotal;
    JBScrollPane objectLayoutScrollPane;
    JButton copyButton;
}
