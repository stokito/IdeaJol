package com.github.stokito.IdeaJol.toolwindow;

import com.github.stokito.IdeaJol.FieldLayoutGap;
import com.github.stokito.IdeaJol.PsiClassAdapter;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.FieldLayout;
import org.openjdk.jol.layouters.Layouter;

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

import static com.github.stokito.IdeaJol.Layouters.LAYOUTERS;
import static com.intellij.ui.JBColor.RED;
import static java.awt.font.TextAttribute.STRIKETHROUGH;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

public class JolView extends SimpleToolWindowPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance("#com.github.stokito.IdeaJol.toolwindow.JolView");
    private Project project;

    private SmartPsiElementPointer<PsiClass> psiClass;
    private JolForm jolForm = new JolForm();

    public JolView(@NotNull Project project) {
        super(true, true);
        this.project = project;
        setupUI();
    }

    private void setupUI() {
        jolForm.tblObjectLayout.getEmptyText().setText("Select a class then press Code / Show Object Layout");
        jolForm.tblObjectLayout.setSelectionMode(SINGLE_SELECTION);
        jolForm.tblObjectLayout.getSelectionModel().addListSelectionListener(this::navigateToFieldInEditor);
        jolForm.lblClassName.addMouseListener(navigateToClassInEditor());
        jolForm.copyButton.addActionListener(this::copyObjectLayoutToClipboard);
        jolForm.cmbDataModel.addActionListener(this::layoutOptionsActionPerformed);
        setContent(jolForm.rootPanel);
    }

    @Override
    public void dispose() {
        project = null;
        psiClass = null;
        jolForm = null;
    }

    public void showLayoutForClass(@NotNull PsiClass psiClass) {
        this.psiClass = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(psiClass);
        classLabelFontStrike(FALSE);
        jolForm.lblClassName.setText(psiClass.getName());
        jolForm.lblClassName.setIcon(psiClass.getIcon(0));
        jolForm.copyButton.setEnabled(true);
        showLayoutForSelectedClass();
    }

    @NotNull
    private Layouter getSelectedLayoter() {
        int layouterIndex = jolForm.cmbDataModel.getSelectedIndex();
        return LAYOUTERS[layouterIndex];
    }

    private void showLayoutForSelectedClass() {
        PsiClass psiClass = getPsiClass();
        if (psiClass == null) {
            return;
        }
        ClassLayout classLayout = calcClassLayout(psiClass);
        ArrayList<FieldLayout> objectLayouts = collectObjectLayouts(classLayout);

        TableModel model = new FieldLayoutTableModel(objectLayouts);
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
    private ArrayList<FieldLayout> collectObjectLayouts(@NotNull ClassLayout classLayout) {
        ArrayList<FieldLayout> objectLines = new ArrayList<>(classLayout.fields().size() + 8);
        objectLines.add(new FieldLayoutGap(0, classLayout.headerSize(), "(object header)"));
        long nextFree = classLayout.headerSize();
        for (FieldLayout fieldLayout : classLayout.fields()) {
            if (fieldLayout.offset() > nextFree) {
                long fieldLayoutSize = fieldLayout.offset() - nextFree;
                objectLines.add(new FieldLayoutGap(nextFree, fieldLayoutSize, "(alignment/padding gap)"));
            }
            objectLines.add(fieldLayout);
            nextFree = fieldLayout.offset() + fieldLayout.size();
        }
        if (classLayout.instanceSize() != nextFree) {
            objectLines.add(new FieldLayoutGap(nextFree, classLayout.getLossesExternal(), "(loss due to the next object alignment)"));
        }

        showTotalInstanceSize(classLayout);
        return objectLines;
    }

    private void showTotalInstanceSize(ClassLayout classLayout) {
        jolForm.lblInstanceSize.setText(Long.toString(classLayout.instanceSize()));
        changeLabelInstanceSizeColorIfLargerThanCacheLine(classLayout.instanceSize());
        jolForm.lblLossesInternal.setText(Long.toString( classLayout.getLossesInternal()));
        jolForm.lblLossesExternal.setText(Long.toString(classLayout.getLossesExternal()));
        jolForm.lblLossesTotal.setText(Long.toString(classLayout.getLossesTotal()));
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
                if (psiClassElement == null) {
                    return;
                }
                psiClassElement.navigate(true);
            }
        };
    }

    private void navigateToFieldInEditor(@NotNull ListSelectionEvent e) {
        int fieldIndex = jolForm.tblObjectLayout.getSelectionModel().getLeadSelectionIndex();
        // first row is always object header
        if (fieldIndex == 0) {
            return;
        }
        // on reset of model the selected index can be more than new count of rows
        if (fieldIndex == -1 || fieldIndex > jolForm.tblObjectLayout.getModel().getRowCount() - 1) {
            return;
        }
        String className = (String) jolForm.tblObjectLayout.getModel().getValueAt(fieldIndex, 3);
        // for padding/gap the class name is null
        if (className == null) {
            return;
        }
        String fieldName = (String) jolForm.tblObjectLayout.getModel().getValueAt(fieldIndex, 4);
        PsiField psiField = findFieldInHierarchy(className, fieldName);
        if (psiField != null) {
            psiField.navigate(true);
        }
    }

    @Nullable
    private PsiField findFieldInHierarchy(@NotNull String className, @Nullable String fieldName) {
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
            if (parentClassName.equals(className) && field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    private void layoutOptionsActionPerformed(@NotNull ActionEvent e) {
        showLayoutForSelectedClass();
    }

    /** Safely get a PsiClass - it can be already removed then we'll keep layout but strike out class name label */
    @Nullable
    private PsiClass getPsiClass() {
        PsiClass psiClassElement = psiClass != null ? psiClass.getElement() : null;
        // QualifiedName == null is a workaround for #20
        if (psiClassElement == null || psiClassElement.getQualifiedName() == null) {
            classLabelFontStrike(TRUE);
            psiClass = null;
            return null;
        }
        return psiClassElement;
    }

    private void classLabelFontStrike(Boolean strikethroughOn) {
        @SuppressWarnings("unchecked")
        Map<TextAttribute, Object> fontAttributes = (Map<TextAttribute, Object>) jolForm.lblClassName.getFont().getAttributes();
        fontAttributes.put(STRIKETHROUGH, strikethroughOn);
        Font strikethroughFont = new Font(fontAttributes);
        jolForm.lblClassName.setFont(strikethroughFont);
    }

    private void copyObjectLayoutToClipboard(@NotNull ActionEvent e) {
        PsiClass psiClass = getPsiClass();
        if (psiClass == null) {
            return;
        }
        ClassLayout classLayout = calcClassLayout(psiClass);
        CopyPasteManager.getInstance().setContents(new StringSelection(classLayout.toPrintable()));
    }

    private ClassLayout calcClassLayout(@NotNull PsiClass psiClass) {
        Layouter layouter = getSelectedLayoter();
        ClassData classData = PsiClassAdapter.createClassDataFromPsiClass(psiClass);
        return layouter.layout(classData);
    }

    public static JolView getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, JolView.class);
    }
}
