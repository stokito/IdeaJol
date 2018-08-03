package com.github.stokito.IdeaJol;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
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

import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
        add(jolForm.rootPanel, BorderLayout.CENTER);
        jolForm.tblObjectLayout.getEmptyText().setText("Select a class then press Code / Show Object Layout");
        jolForm.tblObjectLayout.setDefaultEditor(Object.class, null);
        jolForm.tblObjectLayout.setSelectionMode(SINGLE_SELECTION);
        jolForm.tblObjectLayout.getSelectionModel().addListSelectionListener(this::navigateToFieldInEditor);
        jolForm.lblClassName.addMouseListener(navigateToClassInEditor());
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
        jolForm.lblClassName.setIcon(psiClass.getIcon(0));
        showLayoutForSelectedClass();
    }

    @NotNull
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

        ArrayList<Object[]> objectLines = collectObjectLayouts();

        Object[][] rows = objectLines.toArray(new Object[0][0]);
        DefaultTableModel model = new DefaultTableModel(rows, COLUMNS);
        jolForm.tblObjectLayout.setModel(model);
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

        jolForm.lblInstanceSize.setText(Long.toString(sizeOf));
        jolForm.lblLossesInternal.setText(Long.toString(interLoss));
        jolForm.lblLossesExternal.setText(Long.toString(exterLoss));
        jolForm.lblLossesTotal.setText(Long.toString(totalLoss));
        return objectLines;
    }

    @NotNull
    private MouseAdapter navigateToClassInEditor() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                psiClass.navigate(true);
            }
        };
    }

    //FIXME
    private void navigateToFieldInEditor(ListSelectionEvent e) {
        int fieldIndex = e.getFirstIndex();
        int fieldIndexLst = e.getLastIndex();
        String typeName = (String) jolForm.tblObjectLayout.getModel().getValueAt(fieldIndex, 2);
        String fieldName = (String) jolForm.tblObjectLayout.getModel().getValueAt(fieldIndex, 4);
        System.out.println("selected " + fieldIndex + " " + fieldIndexLst + " " + typeName + " " + fieldName);
        if (fieldName != null) {
            PsiField psiField = psiClass.findFieldByName(fieldName, true);
            if (psiField != null) {
                psiField.navigate(true);
            }
        }
    }

    private void layoutOptionsActionPerformed(ActionEvent e) {
        showLayoutForSelectedClass();
    }

    public static JolView getInstance(Project project) {
        return ServiceManager.getService(project, JolView.class);
    }
}