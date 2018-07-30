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

import javax.swing.*;
import java.awt.*;

public class JolView extends SimpleToolWindowPanel implements Disposable {
    protected final Project project;
    protected final ToolWindowManager toolWindowManager;
    protected final KeymapManager keymapManager;
    private final String extension;

    protected Editor editor;
    protected Document document;
    private JLabel labelClassName;

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

        final JPanel toolbarPanel = new JPanel(new BorderLayout());
        labelClassName = new JLabel("");
        toolbarPanel.add(labelClassName, BorderLayout.CENTER);

        setToolbar(toolbarPanel);
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
        labelClassName.setText(psiClass.getName());
        ClassData classData = PsiClassAdapter.createClassDataFromPsiClass(psiClass);
        StringBuilder output = new StringBuilder(6 * 1024);

        output.append("***** 32-bit VM: **********************************************************\n");
        printLayout(classData, new HotSpotLayouter(new X86_32_DataModel()), output);

        output.append("***** 64-bit VM: **********************************************************\n");
        printLayout(classData, new HotSpotLayouter(new X86_64_DataModel()), output);

        output.append("***** 64-bit VM, compressed references enabled: ***************************\n");
        printLayout(classData, new HotSpotLayouter(new X86_64_COOPS_DataModel()), output);

        output.append("***** 64-bit VM, compressed references enabled, 16-byte align: ************\n");
        printLayout(classData, new HotSpotLayouter(new X86_64_COOPS_DataModel(16)), output);

        document.setText(output);
    }

    private void printLayout(ClassData classData, Layouter layouter, StringBuilder sb) {
        ClassLayout classLayout = layouter.layout(classData);
        String clazzLayout = classLayout.toPrintable();
        sb.append(clazzLayout).append('\n');
    }

    public static JolView getInstance(Project project) {
        return ServiceManager.getService(project, JolView.class);
    }
}