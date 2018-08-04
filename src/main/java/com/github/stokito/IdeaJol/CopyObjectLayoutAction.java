package com.github.stokito.IdeaJol;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.Nullable;
import org.openjdk.jol.datamodel.X86_64_COOPS_DataModel;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;

import java.awt.datatransfer.StringSelection;

public class CopyObjectLayoutAction extends AnAction {
    private static final Logger LOG = Logger.getInstance("#com.github.stokito.IdeaJol.CopyObjectLayoutAction");
    private static final Layouter HOT_SPOT_X64_COOPS_LAYOUTER = new HotSpotLayouter(new X86_64_COOPS_DataModel());

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
            ClassLayout classLayout = HOT_SPOT_X64_COOPS_LAYOUTER.layout(classData);
            CopyPasteManager.getInstance().setContents(new StringSelection(classLayout.toPrintable()));
        } catch (Exception ex) {
            LOG.error("Unable to generate and copy layout", ex);
        }
    }

    @Nullable
    private PsiClass getSelectedPsiClass(AnActionEvent event) {
        Project project = event.getProject();
        Navigatable navigatable = event.getData(CommonDataKeys.NAVIGATABLE);
        return project != null && navigatable instanceof PsiClass ? (PsiClass) navigatable : null;
    }

}
