package com.github.stokito.IdeaJol;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import java.awt.*;

class JolForm {
    JComponent rootPanel;
    JLabel lblInstanceSize;
    JLabel lblClassName;
    JToolBar toolbarPanel;
    JComboBox cmbDataModel;
    JBTable tblObjectLayout;
    JPanel pnlInstanceSize;
    JLabel lblLossesInternal;
    JLabel lblLossesExternal;
    JLabel lblLossesTotal;
    JBScrollPane objectLayoutScrollPane;
    JButton copyButton;
}
