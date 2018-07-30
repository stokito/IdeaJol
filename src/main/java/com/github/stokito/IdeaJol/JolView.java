package com.github.stokito.IdeaJol;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiClass;
import org.openjdk.jol.datamodel.X86_32_DataModel;
import org.openjdk.jol.datamodel.X86_64_COOPS_DataModel;
import org.openjdk.jol.datamodel.X86_64_DataModel;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;
import org.openjdk.jol.layouters.RawLayouter;

import javax.swing.*;

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
    private final String extension;

    protected Editor editor;
    protected Document document;
    private JLabel lblClassName;
    private JPanel toolbarPanel;
    private JComboBox cmbLayouter;
    private JComboBox cmbDataModel;
    private PsiClass psiClass;
    private ClassData classData;

    public JolView(final ToolWindowManager toolWindowManager, KeymapManager keymapManager, final Project project, final String fileExtension) {
        super(true, true);
        this.toolWindowManager = toolWindowManager;
        this.keymapManager = keymapManager;
        this.project = project;
        this.extension = fileExtension;
        setupUI();
    }

    public JolView(final ToolWindowManager toolWindowManager, KeymapManager keymapManager, final Project project) {
        this(toolWindowManager, keymapManager, project, "java");
    }

    private void setupUI() {
        final EditorFactory editorFactory = EditorFactory.getInstance();
        document = editorFactory.createDocument("");
        editor = editorFactory.createEditor(document, project, FileTypeManager.getInstance().getFileTypeByExtension(extension), true);

        final JComponent editorComponent = editor.getComponent();
        add(editorComponent);

        setToolbar(toolbarPanel);

        cmbLayouter.addActionListener(e -> {
            printLayout();
        });
        cmbDataModel.addActionListener(e -> {
            printLayout();
        });
    }

    @Override
    public void dispose() {
        if (editor != null) {
            final EditorFactory editorFactory = EditorFactory.getInstance();
            editorFactory.releaseEditor(editor);
            editor = null;
        }
    }

    public void setOutput(PsiClass psiClass) {
        this.psiClass = psiClass;
        this.classData = PsiClassAdapter.createClassDataFromPsiClass(psiClass);
        lblClassName.setText(psiClass.getName());
        printLayout();
    }

    private Layouter getLayoter() {
        int layouterIndex = (4 * cmbLayouter.getSelectedIndex()) + cmbDataModel.getSelectedIndex();
        return layouters[layouterIndex];
    }

    private void printLayout() {
        if (classData == null) {
            return;
        }
        Layouter layouter = getLayoter();
        ClassLayout classLayout = layouter.layout(classData);
        String clazzLayout = classLayout.toPrintable();
        document.setText(clazzLayout);
    }

    public static JolView getInstance(Project project) {
        return ServiceManager.getService(project, JolView.class);
    }
}