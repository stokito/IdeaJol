package com.github.stokito.IdeaJol.inspection;

import com.github.stokito.IdeaJol.toolwindow.JolView;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class ShowJolQuickFix implements LocalQuickFix {
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
        JolView.showJolToolWindow(project, (PsiClass) psiClass);
    }
}
