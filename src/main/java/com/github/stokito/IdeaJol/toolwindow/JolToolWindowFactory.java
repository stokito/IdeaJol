package com.github.stokito.IdeaJol.toolwindow;

import com.github.stokito.IdeaJol.JolView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class JolToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JComponent jol = JolView.getInstance(project);
        toolWindow.getContentManager().addContent(ContentFactory.SERVICE.getInstance().createContent(jol, "Object Layout", false));
    }
}
