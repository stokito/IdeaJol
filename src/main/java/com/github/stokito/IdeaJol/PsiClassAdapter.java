package com.github.stokito.IdeaJol;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.FieldData;

import static com.intellij.psi.PsiModifier.STATIC;

class PsiClassAdapter {
    @NotNull
    static ClassData createClassDataFromPsiClass(@NotNull PsiClass psiClass) {
        ClassData classData = new ClassData(psiClass.getQualifiedName());
        if (psiClass.getSuperClass() != null) {
            ClassData supperClassData = createClassDataFromPsiClass(psiClass.getSuperClass());
            classData.addSuperClassData(supperClassData);
        }
        do {
            for (PsiField psiField : psiClass.getFields()) {
                if (psiField.getModifierList().hasModifierProperty(STATIC)) { // skip static fields
                    continue;
                }
                String typeText = psiField.getType().getPresentableText();
                String contendedGroup = fetchContendedGroup(psiField, "sun.misc.Contended");
                if (contendedGroup == null) {
                    contendedGroup = fetchContendedGroup(psiField, "jdk.internal.vm.annotation.Contended");
                }
                boolean isContended = contendedGroup != null;
                FieldData fieldData = new FieldData(null, -1L, psiClass.getQualifiedName(), psiField.getName(), typeText, isContended, contendedGroup);
                classData.addField(fieldData);
            }
            classData.addSuperClass(psiClass.getQualifiedName());
        } while ((psiClass = psiClass.getSuperClass()) != null);
        return classData;
    }

    /**
     * @return null if field is not marked with contended annotation, empty string for default group or name of contended group
     */
    @Nullable
    private static String fetchContendedGroup(@NotNull PsiField psiField,  @NotNull String annotationClass) {
        PsiAnnotation annotation = psiField.getAnnotation(annotationClass);
        String contendedGroup = annotation != null ? getAnnotationStrValue(annotation) : null;
        return contendedGroup;
    }

    private static String getAnnotationStrValue(@NotNull PsiAnnotation annotation) {
        PsiAnnotationMemberValue annotationValue = annotation.findAttributeValue("value");
        String contendedGroup = annotationValue != null ? (String) ((PsiLiteralExpression) annotationValue).getValue() : null;
        return contendedGroup;
    }
}
