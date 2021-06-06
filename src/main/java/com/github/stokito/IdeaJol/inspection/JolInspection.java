package com.github.stokito.IdeaJol.inspection;

import com.github.stokito.IdeaJol.PsiClassAdapter;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ui.ListTable;
import com.intellij.codeInspection.ui.ListWrappingTableModel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.util.ui.FormBuilder;
import com.siyeh.ig.ui.UiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UClass;
import org.openjdk.jol.datamodel.X86_64_COOPS_DataModel;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.layouters.Layouter;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.github.stokito.IdeaJol.Layouters.LAYOUTERS;
import static com.intellij.codeInspection.ProblemHighlightType.WEAK_WARNING;
import static java.util.Arrays.asList;
import static org.jetbrains.uast.UElementKt.getSourcePsiElement;

public class JolInspection extends AbstractBaseUastLocalInspectionTool {
    private static final Logger LOG = Logger.getInstance("#com.github.stokito.IdeaJol.inspection.JolInspection");

    private static final LocalQuickFix SHOW_JOL_QUICK_FIX = new ShowJolQuickFix();

    /**
     * Suffixes of ignored business logic classes
     */
    @SuppressWarnings("WeakerAccess")
    public List<String> businessLogicClassSuffixes = asList(
            "Exception", "Test", "Spec", "Impl", "Dao", "Utils",
            "Controller", "Service", "Strategy", "Servlet", "Adapter", "Factory", "Provider",
            "Handler", "Registry", "Filter", "Interceptor", "Executor"
    );

    /**
     * Default 5 is Hotspot COOPS
     * @see X86_64_COOPS_DataModel
     */
    @SuppressWarnings("WeakerAccess")
    public int selectedLayouter = 5;

    /**
     * Class memory size threshold (CPU cache line is 64)
     * Default 64 is a one cache line
     */
    @SuppressWarnings("WeakerAccess")
    public int sizeThreshold = 64;

    @Nullable
    @Override
    public ProblemDescriptor[] checkClass(@NotNull UClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
        if (isNotUsualClass(aClass) || isBusinessLogicClass(aClass)) {
            return null;
        }
        // Workaround for #20 NPE
        if (aClass.getQualifiedName() == null) {
//            LOG.warn("The class doesn't have a qualified name: " + aClass);
            return null;
        }
        ClassData classData = PsiClassAdapter.createClassDataFromPsiClass(aClass);
        ClassLayout layout = getLayouter().layout(classData);
        if (layout.instanceSize() <= sizeThreshold) {
            return null;
        }
        PsiElement navigateTo = getSourcePsiElement(aClass);
        if (navigateTo == null) {
            // this shouldn't happen because we already have this check inside of isNotUsualClass()
            return null;
        }
        ProblemDescriptor problem = manager.createProblemDescriptor(navigateTo, "Class has too big memory footprint", SHOW_JOL_QUICK_FIX, WEAK_WARNING, isOnTheFly);
        return new ProblemDescriptor[]{problem};
    }

    private Layouter getLayouter() {
        return LAYOUTERS[selectedLayouter];
    }

    /**
     * @return true if class is anonymous, annotation, enum, interface, or is not exists in source
     */
    private boolean isNotUsualClass(PsiClass aClass) {
        return aClass.getNameIdentifier() == null ||
                aClass.isAnnotationType() ||
                aClass.isEnum() ||
                aClass.isInterface();
//FIXME: for kotlin this always true                || !aClass.isPhysical();
    }

    private boolean isBusinessLogicClass(PsiClass aClass) {
        return endsWithAny(aClass.getName(), businessLogicClassSuffixes);
    }

    private boolean endsWithAny(String className, List<String> businessLogicClassSuffixes) {
        if (className == null) return false;
        for (String suffix : businessLogicClassSuffixes) {
            if (className.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

/* looks like this approach works slower, needs to be better investigated with JMH
    private static final Set<String> BUSINESS_LOGIC_CLASS_SUFFIXES_SET = new HashSet<>(Arrays.asList(businessLogicClassSuffixes));

    private int indexOfLastCamelCaseWord(String className) {
        for (int i = className.length() - 1; i >= 0; i--) {
            if (Character.isUpperCase(className.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean endsWithAnyWord(String className) {
        int pos = indexOfLastCamelCaseWord(className);
        if (pos == -1) {
            return false;
        }
        String lastWordOfClassName = className.substring(pos);
        return BUSINESS_LOGIC_CLASS_SUFFIXES_SET.contains(lastWordOfClassName);
    }
*/

    @Nullable
    @Override
    public JComponent createOptionsPanel() {
        IntegerField sizeThresholdEditor = new IntegerField(null, 24, Integer.MAX_VALUE);
        sizeThresholdEditor.getValueEditor().addListener(newValue -> sizeThreshold = newValue);
        sizeThresholdEditor.setValue(sizeThreshold);
        sizeThresholdEditor.setColumns(4);
        sizeThresholdEditor.setToolTipText("Class memory size threshold (CPU cache line is 64)");

        String[] layouterNames = Stream.of(LAYOUTERS).map(Object::toString).toArray(String[]::new);
        final ComboBox<String> layouterComboBox = new ComboBox<>(layouterNames);
        layouterComboBox.setSelectedIndex(selectedLayouter);
        layouterComboBox.addActionListener(e -> selectedLayouter = layouterComboBox.getSelectedIndex());
        layouterComboBox.setToolTipText("Almost everywhere used HotSpot 64x COOPS. Raw layouter shows size of the fields themselves");

        final ListTable businessLogicClassSuffixesTable = new ListTable(new ListWrappingTableModel(businessLogicClassSuffixes, "Suffixes of ignored business logic classes"));
        final JPanel businessLogicClassSuffixesTablePanel = UiUtils.createAddRemovePanel(businessLogicClassSuffixesTable);

        return new FormBuilder()
                .addLabeledComponent("Memory size threshold", sizeThresholdEditor)
                .addLabeledComponent("Layouter", layouterComboBox)
                .addComponentFillVertically(businessLogicClassSuffixesTablePanel, 0)
                .getPanel();
    }
}
