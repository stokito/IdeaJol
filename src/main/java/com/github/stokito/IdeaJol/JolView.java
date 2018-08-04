package com.github.stokito.IdeaJol;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.Map;

import static com.intellij.ui.JBColor.RED;
import static java.awt.font.TextAttribute.STRIKETHROUGH;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
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

    protected final Project project;
    protected final ToolWindowManager toolWindowManager;
    protected final KeymapManager keymapManager;

    private SmartPsiElementPointer<PsiClass> psiClass;
    private ClassData classData;
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
        jolForm.copyButton.addActionListener(this::copyObjectLayoutToClipboard);
        jolForm.cmbLayouter.addActionListener(this::layoutOptionsActionPerformed);
        jolForm.cmbDataModel.addActionListener(this::layoutOptionsActionPerformed);
    }

    @Override
    public void dispose() {
    }

    public void showLayoutForClass(PsiClass psiClass) {
        this.psiClass = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(psiClass);
        this.classData = PsiClassAdapter.createClassDataFromPsiClass(psiClass);
        classLabelFontStrike(FALSE);
        jolForm.lblClassName.setText(psiClass.getName());
        jolForm.lblClassName.setIcon(psiClass.getIcon(0));
        jolForm.copyButton.setEnabled(true);
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
        ClassLayout classLayout = layouter.layout(classData);
        ArrayList<Object[]> objectLines = collectObjectLayouts(classLayout);

        Object[][] rows = objectLines.toArray(new Object[0][0]);
        TableModel model = new FieldLayoutTableModel(rows);
        jolForm.tblObjectLayout.setModel(model);
        TableColumnModel columnModel = jolForm.tblObjectLayout.getColumnModel();
        columnModel.getColumn(0).setMaxWidth(50);
        columnModel.getColumn(0).setResizable(false);
        columnModel.getColumn(1).setMaxWidth(50);
        columnModel.getColumn(1).setResizable(false);
    }

    /**
     * Convert classLayout to rows of table.
     * TODO: This should be already done in classLayout so we shouldn't make any calculations
     */
    @NotNull
    private ArrayList<Object[]> collectObjectLayouts(ClassLayout classLayout) {
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

        showTotalInstanceSize(interLoss, exterLoss, sizeOf, totalLoss);
        return objectLines;
    }

    private void showTotalInstanceSize(long interLoss, long exterLoss, long sizeOf, long totalLoss) {
        jolForm.lblInstanceSize.setText(Long.toString(sizeOf));
        changeLabelInstanceSizeColorIfLargerThanCacheLine(sizeOf);
        jolForm.lblLossesInternal.setText(Long.toString(interLoss));
        jolForm.lblLossesExternal.setText(Long.toString(exterLoss));
        jolForm.lblLossesTotal.setText(Long.toString(totalLoss));
    }

    /** Processor cache line is almost always 64 bytes */
    private void changeLabelInstanceSizeColorIfLargerThanCacheLine(long sizeOf) {
        if (sizeOf > 64) {
            jolForm.lblInstanceSize.setForeground(RED);
            jolForm.lblInstanceSize.setToolTipText("More that 64 bytes of cache line and this is bad for performance");
        } else {
            // copy default label color from another label
            jolForm.lblInstanceSize.setForeground(jolForm.lblLossesExternal.getForeground());
            jolForm.lblInstanceSize.setToolTipText(null);
        }
    }

    @NotNull
    private MouseAdapter navigateToClassInEditor() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                PsiClass psiClassElement = getPsiClass();
                if (psiClassElement != null) {
                    psiClassElement.navigate(true);
                }
            }
        };
    }

    private void navigateToFieldInEditor(ListSelectionEvent e) {
        int fieldIndex = jolForm.tblObjectLayout.getSelectionModel().getLeadSelectionIndex();
        // on reset of model the selected index can be more than new count of rows
        if (fieldIndex == -1 || fieldIndex > jolForm.tblObjectLayout.getModel().getRowCount() - 1) {
            return;
        }
        String className = (String) jolForm.tblObjectLayout.getModel().getValueAt(fieldIndex, 3);
        String fieldName = (String) jolForm.tblObjectLayout.getModel().getValueAt(fieldIndex, 4);
        PsiField psiField = findField(className, fieldName);
        if (psiField != null) {
            psiField.navigate(true);
        }
    }

    @Nullable
    private PsiField findField(String className, String fieldName) {
        if (fieldName == null) {
            return null;
        }
        PsiClass psiClassElement = getPsiClass();
        if (psiClassElement == null) {
            return null;
        }
        for (PsiField field : psiClassElement.getAllFields()) {
            PsiClass parentClass = (PsiClass) field.getParent();
            String parentClassName = parentClass.getName();
            assert parentClassName != null;
            assert field.getName() != null;
            if (parentClassName.equals(className) && field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    private void layoutOptionsActionPerformed(ActionEvent e) {
        showLayoutForSelectedClass();
    }

    @Nullable
    private PsiClass getPsiClass() {
        PsiClass psiClassElement = psiClass != null ? psiClass.getElement() : null;
        if (psiClassElement == null) {
            classLabelFontStrike(TRUE);
            psiClass = null;
        }
        return psiClassElement;
    }

    private void classLabelFontStrike(Boolean strikethroughOn) {
        @SuppressWarnings("unchecked")
        Map<TextAttribute, Object> fontAttributes = (Map<TextAttribute, Object>) jolForm.lblClassName.getFont().getAttributes();
        fontAttributes.put(STRIKETHROUGH, strikethroughOn);
        Font strikedFont = new Font(fontAttributes);
        jolForm.lblClassName.setFont(strikedFont);
    }

    private void copyObjectLayoutToClipboard(ActionEvent e) {
        Layouter layouter = getSelectedLayoter();
        ClassLayout classLayout = layouter.layout(classData);
        CopyPasteManager.getInstance().setContents(new StringSelection(classLayout.toPrintable()));
    }

    public static JolView getInstance(Project project) {
        return ServiceManager.getService(project, JolView.class);
    }
}