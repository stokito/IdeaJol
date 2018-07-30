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
import org.openjdk.jol.datamodel.X86_32_DataModel;
import org.openjdk.jol.datamodel.X86_64_COOPS_DataModel;
import org.openjdk.jol.datamodel.X86_64_DataModel;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;

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
            ClassData classData = PsiClassAdapter.createClassDataFromPsiClass(psiClass);
            StringBuilder sb = new StringBuilder(6 * 1024);

            sb.append("***** 32-bit VM: **********************************************************\n");
            printLayout(classData, new HotSpotLayouter(new X86_32_DataModel()), sb);

            sb.append("***** 64-bit VM: **********************************************************\n");
            printLayout(classData, new HotSpotLayouter(new X86_64_DataModel()), sb);

            sb.append("***** 64-bit VM, compressed references enabled: ***************************\n");
            printLayout(classData, new HotSpotLayouter(new X86_64_COOPS_DataModel()), sb);

            sb.append("***** 64-bit VM, compressed references enabled, 16-byte align: ************\n");
            printLayout(classData, new HotSpotLayouter(new X86_64_COOPS_DataModel(16)), sb);

            JolView.getInstance(project).setOutput(psiClass.getName(), sb);
            ToolWindowManager.getInstance(project).getToolWindow("JOL").activate(null);
        } catch (Exception ex) {
            LOG.error("Unable to generate layout", ex);
        }
    }

    private void printLayout(ClassData classData, Layouter layouter, StringBuilder sb) {
        ClassLayout classLayout = layouter.layout(classData);
        String clazzLayout = classLayout.toPrintable();
        sb.append(clazzLayout).append('\n');
    }

    @Nullable
    private PsiClass getSelectedPsiClass(AnActionEvent event) {
        Project project = event.getProject();
        Navigatable navigatable = event.getData(CommonDataKeys.NAVIGATABLE);
        return project != null && navigatable instanceof PsiClass ? (PsiClass) navigatable : null;
    }

}
