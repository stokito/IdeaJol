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

    public void setOutput(String className, CharSequence output) {
        labelClassName.setText(className);
        document.setText(output);
    }

    public static JolView getInstance(Project project) {
        return ServiceManager.getService(project, JolView.class);
    }
}