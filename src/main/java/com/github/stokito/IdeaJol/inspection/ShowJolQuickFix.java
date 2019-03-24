package com.github.stokito.IdeaJol.inspection;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.actionSystem.ActionPlaces.UNKNOWN;
import static org.jetbrains.annotations.Nls.Capitalization.Sentence;

public class ShowJolQuickFix implements LocalQuickFix {
    @Nls(capitalization = Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
        return "Show Object Layout";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
        PsiElement psiClass = problemDescriptor.getPsiElement();
        if (!(psiClass instanceof PsiClass)) {
            return;
        }
        ActionManager am = ActionManager.getInstance();
        DataContext dataContext = DataManager.getInstance().getDataContext();
        Presentation presentation = new Presentation();
        AnActionEvent anActionEvent = new AnActionEvent(null, dataContext, UNKNOWN, presentation, am, 0);
        am.getAction("showObjectLayout").actionPerformed(anActionEvent);

    }
}
