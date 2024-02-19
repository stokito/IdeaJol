package com.github.stokito.IdeaJol;

import com.github.stokito.IdeaJol.toolwindow.JolView;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UastContextKt;

import static com.intellij.openapi.actionSystem.CommonDataKeys.NAVIGATABLE;

public class ShowObjectLayoutAction extends AnAction {
    private static final Logger LOG = Logger.getInstance("#com.github.stokito.IdeaJol.ShowObjectLayoutAction");

    @Override
    public void update(@NotNull AnActionEvent event) {
        PsiClass selectedPsiClass = getSelectedPsiClass(event);
        event.getPresentation().setEnabledAndVisible(selectedPsiClass != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        assert project != null;
        PsiClass psiClass = getSelectedPsiClass(event);
        if (psiClass == null) { //FIXME
            LOG.error("Can't show layout: unable to determine selected class. Did you selected the class name?");
            return;
        }
        JolView.showJolToolWindow(project, psiClass);
    }

    @Nullable
    private PsiClass getSelectedPsiClass(AnActionEvent event) {
        Project project = event.getProject();
        Navigatable navigatable = event.getData(NAVIGATABLE);
        if (project == null || !(navigatable instanceof PsiElement)) {
            return null;
        }
        // Plain Java class
        if (navigatable instanceof PsiClass) {
            return (PsiClass) navigatable;
        }
        // Kotlin classes
        UElement element = UastContextKt.toUElement((PsiElement) navigatable);
        if (element instanceof UClass) {
            return ((UClass)element);
        }
        return null;
    }

}
