package com.github.stokito.IdeaJol;

import com.intellij.codeInspection.*;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.layouters.Layouter;

public class JolInspection extends AbstractBaseJavaLocalInspectionTool {
    private static final String[] BUSINESS_LOGIC_CLASS_MARKERS = {"Controller", "Service", "Strategy", "Adapter", "Exception", "Impl", "Dao"};
    private static final Layouter LAYOUTER = Layouters.LAYOUTERS[5]; // hotspot coops
    private static final long THRESHOLD = 64;
    private static final LocalQuickFix SHOW_JOL_QUICK_FIX = new ShowJolQuickFix();

    @Nullable
    @Override
    public ProblemDescriptor[] checkClass(@NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
        if (isNotUsualClass(aClass) || isBusinessLogicClass(aClass)) {
            return null;
        }
        ClassData classData = PsiClassAdapter.createClassDataFromPsiClass(aClass);
        ClassLayout layout = LAYOUTER.layout(classData);
        if (layout.instanceSize() <= THRESHOLD) {
            return null;
        }
        ProblemDescriptor problem = manager.createProblemDescriptor(aClass, "Class have too big memory footprint", SHOW_JOL_QUICK_FIX, ProblemHighlightType.WEAK_WARNING, isOnTheFly);
        return new ProblemDescriptor[]{problem};
    }

    /**
     * @return true if class is anonymous, annotation, enum, interface, or is not exists in source
     */
    private boolean isNotUsualClass(PsiClass aClass) {
        return aClass.getNameIdentifier() == null ||
                aClass.isAnnotationType() ||
                aClass.isEnum() ||
                aClass.isInterface() ||
                !aClass.isPhysical();
    }

    private boolean isBusinessLogicClass(PsiClass aClass) {
        return endsWithAny(aClass.getName(), BUSINESS_LOGIC_CLASS_MARKERS);
    }

    // can be replaced with commons.lang3.StringUtils.endsWithAny
    private boolean endsWithAny(String className, String[] businessLogicClassMarkers) {
        if (className == null) return false;
        for (String marker : businessLogicClassMarkers) {
            if (className.endsWith(marker)) {
                return true;
            }
        }
        return false;
    }

/* looks like this approach works slower, needs to be better investigated with JMH
    private static final Set<String> BUSINESS_LOGIC_CLASS_MARKERS_SET = new HashSet<>(Arrays.asList(BUSINESS_LOGIC_CLASS_MARKERS));

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
        return BUSINESS_LOGIC_CLASS_MARKERS_SET.contains(lastWordOfClassName);
    }
*/

}
