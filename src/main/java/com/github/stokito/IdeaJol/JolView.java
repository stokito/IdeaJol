package com.github.stokito.IdeaJol;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jol.datamodel.X86_32_DataModel;
import org.openjdk.jol.datamodel.X86_64_COOPS_DataModel;
import org.openjdk.jol.datamodel.X86_64_DataModel;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.FieldLayout;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;
import org.openjdk.jol.layouters.RawLayouter;

import javax.swing.table.DefaultTableModel;
import java.awt.*;
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

    private PsiClass psiClass;
    private ClassData classData;
    private ClassLayout classLayout;
    private static final String MSG_GAP = "(alignment/padding gap)";
    private static final String MSG_NEXT_GAP = "(loss due to the next object alignment)";
    private final JolForm jolForm = new JolForm();

    public JolView(final ToolWindowManager toolWindowManager, KeymapManager keymapManager, final Project project) {
        super(true, true);
        this.toolWindowManager = toolWindowManager;
        this.keymapManager = keymapManager;
        this.project = project;
        setupUI();
    }

    private void setupUI() {
//        super(new BorderLayout());
        add(jolForm.rootPanel, BorderLayout.CENTER);
//        setToolbar(toolbarPanel);
        jolForm.tblObjectLayout.getEmptyText().setText("Select a class then press Code / Show Object Layout");
        jolForm.tblObjectLayout.setSelectionMode(SINGLE_SELECTION);
        jolForm.tblObjectLayout.setRowSelectionAllowed(true);
        jolForm.tblObjectLayout.getSelectionModel().addListSelectionListener(e -> {
            System.out.println("selected " + e.getFirstIndex());
            //TODO navigate to PSI class in editor
        });

        jolForm.cmbLayouter.addActionListener(this::layoutOptionsActionPerformed);
        jolForm.cmbDataModel.addActionListener(this::layoutOptionsActionPerformed);
    }

    @Override
    public void dispose() {
    }

    public void showLayoutForClass(PsiClass psiClass) {
        this.psiClass = psiClass;
        this.classData = PsiClassAdapter.createClassDataFromPsiClass(psiClass);
        jolForm.lblClassName.setText(psiClass.getName());
        showLayoutForSelectedClass();
    }

    private Layouter getSelectedLayoter() {
        // we have 4 datamodels for each layouter. This can be replaced with two dimensional array but this more fun
        int layouterIndex = (4 * jolForm.cmbLayouter.getSelectedIndex()) + jolForm.cmbDataModel.getSelectedIndex();
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
        jolForm.tblObjectLayout.setModel(model);
/*TODO configure width of columns
        tblObjectLayout.getColumnModel().getColumn(0).setWidth(60);
        tblObjectLayout.getColumnModel().getColumn(1).setWidth(50);
        tblObjectLayout.getColumnModel().getColumn(2).setPreferredWidth(250);*/
        jolForm.tblObjectLayout.repaint();
    }

    /**
     * Convert classLayout to rows of table.
     * TODO: This should be already done in classLayout so we shouldn't make any calculations
     */
    @NotNull
    private ArrayList<Object[]> collectObjectLayouts() {
        ArrayList<Object[]> objectLines = new ArrayList<>(classLayout.fields().size() + 8);
        objectLines.add(new Object[]{0, classLayout.headerSize(), null, null, "(object header)"});
        long nextFree = classLayout.headerSize();
        long interLoss = 0;
        long exterLoss = 0;
        for (FieldLayout fieldLayout : classLayout.fields()) {
            if (fieldLayout.offset() > nextFree) {
                long fieldLayoutSize = fieldLayout.offset() - nextFree;
                objectLines.add(new Object[]{nextFree, fieldLayoutSize, null, null, MSG_GAP});
                interLoss += fieldLayoutSize;
            }
            objectLines.add(new Object[]{fieldLayout.offset(), fieldLayout.size(), fieldLayout.typeClass(), fieldLayout.classShortName(), fieldLayout.name()});
            nextFree = fieldLayout.offset() + fieldLayout.size();
        }
        long sizeOf = classLayout.instanceSize();
        if (sizeOf != nextFree) {
            exterLoss = sizeOf - nextFree;
            objectLines.add(new Object[]{nextFree, exterLoss, null, null, MSG_NEXT_GAP});
        }
        long totalLoss = interLoss + exterLoss;

        jolForm.lblInstanceSize.setText(Long.toString(totalLoss));
        jolForm.lblLossesInternal.setText(Long.toString(totalLoss));
        jolForm.lblLossesExternal.setText(Long.toString(totalLoss));
        jolForm.lblLossesTotal.setText(Long.toString(totalLoss));
        return objectLines;
    }

    private void layoutOptionsActionPerformed(ActionEvent e) {
        showLayoutForSelectedClass();
    }

    public static JolView getInstance(Project project) {
        return ServiceManager.getService(project, JolView.class);
    }
}