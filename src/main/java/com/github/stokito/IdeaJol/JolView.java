package com.github.stokito.IdeaJol;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jol.datamodel.X86_32_DataModel;
import org.openjdk.jol.datamodel.X86_64_COOPS_DataModel;
import org.openjdk.jol.datamodel.X86_64_DataModel;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.FieldData;
import org.openjdk.jol.info.FieldLayout;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;
import org.openjdk.jol.layouters.RawLayouter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

public class JolView extends SimpleToolWindowPanel implements Disposable {
    private static final int HOTSPOT_DEFAULT_FIELD_ALLOCATION_STYLE = 1;
    private static final X86_32_DataModel MODEL_32 = new X86_32_DataModel();
    private static final X86_64_DataModel MODEL_64 = new X86_64_DataModel();
    private static final X86_64_COOPS_DataModel MODEL_64_COOPS = new X86_64_COOPS_DataModel();
    private static final X86_64_COOPS_DataModel MODEL_64_COOPS_16 = new X86_64_COOPS_DataModel(16);
    private static final Layouter[] layouters = {
            new RawLayouter(MODEL_32),
            new RawLayouter(MODEL_64),
            new RawLayouter(MODEL_64_COOPS),
            new RawLayouter(MODEL_64_COOPS_16),
            new HotSpotLayouter(MODEL_32, false, false, false, true, HOTSPOT_DEFAULT_FIELD_ALLOCATION_STYLE),
            new HotSpotLayouter(MODEL_64, false, false, false, true, HOTSPOT_DEFAULT_FIELD_ALLOCATION_STYLE),
            new HotSpotLayouter(MODEL_64_COOPS, false, false, false, true, HOTSPOT_DEFAULT_FIELD_ALLOCATION_STYLE),
            new HotSpotLayouter(MODEL_64_COOPS_16, false, false, false, true, HOTSPOT_DEFAULT_FIELD_ALLOCATION_STYLE)
    };
    private static final Object[] COLUMNS = {"Offset", "Size", "Type", "Class", "Field"};

    protected final Project project;
    protected final ToolWindowManager toolWindowManager;
    protected final KeymapManager keymapManager;

    private JLabel lblClassName;
    private JToolBar toolbarPanel;
    private JComboBox cmbLayouter;
    private JComboBox cmbDataModel;
    private JBTable tblObjectLayout;
    private PsiClass psiClass;
    private ClassData classData;
    private ClassLayout classLayout;
    private static final String MSG_GAP = "(alignment/padding gap)";
    private static final String MSG_NEXT_GAP = "(loss due to the next object alignment)";

    public JolView(final ToolWindowManager toolWindowManager, KeymapManager keymapManager, final Project project) {
        super(true, true);
        this.toolWindowManager = toolWindowManager;
        this.keymapManager = keymapManager;
        this.project = project;
        setupUI();
    }

    private void setupUI() {
        setToolbar(toolbarPanel);
        tblObjectLayout = new JBTable();
        tblObjectLayout.getEmptyText().setText("Select a class then press Code / Show Object Layout");
        tblObjectLayout.setFillsViewportHeight(true);
        tblObjectLayout.setSelectionMode(SINGLE_SELECTION);
        tblObjectLayout.setRowSelectionAllowed(true);
        //Create the scroll pane and add the table to it. Add the scroll pane to this ToolWindowPanel.
        add(new JBScrollPane(tblObjectLayout));
        tblObjectLayout.getSelectionModel().addListSelectionListener(e -> {
            System.out.println("selected " + e.getFirstIndex());
            /*TODO navigate to PSI class in editor */
        });


        cmbLayouter.addActionListener(this::layoutOptionsActionPerformed);
        cmbDataModel.addActionListener(this::layoutOptionsActionPerformed);
    }

    @Override
    public void dispose() {
    }

    public void showLayoutForClass(PsiClass psiClass) {
        this.psiClass = psiClass;
        this.classData = PsiClassAdapter.createClassDataFromPsiClass(psiClass);
        lblClassName.setText(psiClass.getName());
        showLayoutForSelectedClass();
    }

    private Layouter getSelectedLayoter() {
        // we have 4 datamodels for each layouter. This can be replaced with two dimensional array but this more fun
        int layouterIndex = (4 * cmbLayouter.getSelectedIndex()) + cmbDataModel.getSelectedIndex();
        return layouters[layouterIndex];
    }

    private void showLayoutForSelectedClass() {
        if (classData == null) {
            return;
        }
        Layouter layouter = getSelectedLayoter();
        classLayout = layouter.layout(classData);
//        String clazzLayout = classLayout.toPrintable();

        ArrayList<Object[]> objectLines = collectObjectLayouts();

        Object[][] rows = objectLines.toArray(new Object[0][0]);
        DefaultTableModel model = new DefaultTableModel(rows, COLUMNS);
        tblObjectLayout.setModel(model);
/*TODO configure width of columns
        tblObjectLayout.getColumnModel().getColumn(0).setWidth(60);
        tblObjectLayout.getColumnModel().getColumn(1).setWidth(50);
        tblObjectLayout.getColumnModel().getColumn(2).setPreferredWidth(250);*/
        tblObjectLayout.repaint();
    }

    /**
     * Convert classLayout to rows of table.
     * TODO: This should be already done in classLayout so we shouldn't make any calculations
     */
    @NotNull
    private ArrayList<Object[]> collectObjectLayouts() {
        ArrayList<Object[]> objectLines = new ArrayList<>(classLayout.fields().size() + 4);

        long nextFree = classLayout.headerSize();

        long interLoss = 0;
        long exterLoss = 0;
        for (FieldLayout fieldLayout : classLayout.fields()) {
            if (fieldLayout.offset() > nextFree) {
                long fieldLayoutSize = fieldLayout.offset() - nextFree;
//                String nodeText = String.format(" %6d %5d %" + maxTypeLen + "s %-" + maxDescrLen + "s", nextFree, fieldLayoutSize, "", MSG_GAP);
                objectLines.add(new Object[]{nextFree, fieldLayoutSize, "", "", MSG_GAP});

                interLoss += fieldLayoutSize;
            }

/*
            String nodeText = String.format(" %6d %5d %" + maxTypeLen + "s %-" + maxDescrLen + "s",
                    fieldLayout.offset(),
                    fieldLayout.size(),
                    fieldLayout.typeClass(),
                    fieldLayout.shortFieldName()
            );
*/
            objectLines.add(new Object[]{fieldLayout.offset(), fieldLayout.size(), fieldLayout.typeClass(), fieldLayout.classShortName(), fieldLayout.name()});

            nextFree = fieldLayout.offset() + fieldLayout.size();
        }

        long sizeOf = classLayout.instanceSize();

        if (sizeOf != nextFree) {
            exterLoss = sizeOf - nextFree;
//            String nodeText = String.format(" %6d %5s %" + maxTypeLen + "s %s", nextFree, exterLoss, "", MSG_NEXT_GAP);
            objectLines.add(new Object[]{nextFree, exterLoss, null, null, MSG_NEXT_GAP});
        }

//        appendNode(String.format("Instance size: %d bytes%n", sizeOf));
        objectLines.add(new Object[]{null, null, null, "Instance size", sizeOf});

        long totalLoss = interLoss + exterLoss;
//        appendNode(String.format("Space losses: %d bytes internal + %d bytes  = %d bytes total%n", interLoss, exterLoss, totalLoss));
        objectLines.add(new Object[]{null, null, null, "Losses internal", interLoss});
        objectLines.add(new Object[]{null, null, null, "Losses external", exterLoss});
        objectLines.add(new Object[]{null, null, null, "Losses total", totalLoss});
        return objectLines;
    }

    private void layoutOptionsActionPerformed(ActionEvent e) {
        showLayoutForSelectedClass();
    }

    public static JolView getInstance(Project project) {
        return ServiceManager.getService(project, JolView.class);
    }
}