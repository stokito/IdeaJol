package com.github.stokito.IdeaJol;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.Nullable;

public class ShowObjectLayoutAction extends AnAction {
    private static final Logger LOG = Logger.getInstance("#com.github.stokito.IdeaJol.ShowObjectLayoutAction");

    @Override
    public void update(AnActionEvent event) {
        PsiClass selectedPsiClass = getSelectedPsiClass(event);
        event.getPresentation().setEnabled(selectedPsiClass != null);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        assert project != null;
        PsiClass psiClass = getSelectedPsiClass(event);
        assert psiClass != null;
        try {
            JolView.getInstance(project).showLayoutForClass(psiClass);
            ToolWindowManager.getInstance(project).getToolWindow("JOL").activate(null);
        } catch (Exception ex) {
            LOG.error("Unable to generate layout", ex);
        }
    }

    @Nullable
    private PsiClass getSelectedPsiClass(AnActionEvent event) {
        Project project = event.getProject();
        Navigatable navigatable = event.getData(CommonDataKeys.NAVIGATABLE);
        return project != null && navigatable instanceof PsiClass ? (PsiClass) navigatable : null;
    }

}
