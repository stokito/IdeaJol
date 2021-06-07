package com.github.stokito.IdeaJol;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.FieldData;

import static com.intellij.psi.PsiModifier.STATIC;

public class PsiClassAdapter {
    /**
     * Reimplemented logic from org.openjdk.jol.info.ClassData#parse(java.lang.Object, java.lang.Class)
     */
    @NotNull
    public static ClassData createClassDataFromPsiClass(@NotNull PsiClass psiClass) {
        assert psiClass.getQualifiedName() != null : "The class doesn't have a qualified name: " + psiClass;
        ClassData classData = new ClassData(psiClass.getQualifiedName());
        if (psiClass.getSuperClass() != null && !psiClass.equals(psiClass.getSuperClass())) {
            ClassData supperClassData = createClassDataFromPsiClass(psiClass.getSuperClass());
            classData.addSuperClassData(supperClassData);
        }
        addClassFields(psiClass, classData);
        return classData;
    }

    private static void addClassFields(@NotNull PsiClass psiClass, @NotNull final ClassData classData) {
        do {
            for (PsiField psiField : psiClass.getFields()) {
                if (psiField.hasModifierProperty(STATIC)) { // skip static fields
                    continue;
                }
                String typeText = psiField.getType().getPresentableText();
                String contendedGroup = determineContendedGroup(psiField);
                boolean isContended = contendedGroup != null;
                FieldData fieldData = FieldData.create(psiClass.getQualifiedName(), psiField.getName(), typeText, isContended, contendedGroup);
                classData.addField(fieldData);
            }
            assert psiClass.getQualifiedName() != null : "The class doesn't have a qualified name: " + psiClass;
            classData.addSuperClass(psiClass.getQualifiedName());
        } while ((psiClass = psiClass.getSuperClass()) != null);
    }

    @Nullable
    private static String determineContendedGroup(@NotNull PsiField psiField) {
        String contendedGroup = fetchContendedGroup(psiField, "jdk.internal.vm.annotation.Contended");
        if (contendedGroup == null) {
            contendedGroup = fetchContendedGroup(psiField, "sun.misc.Contended");
        }
        return contendedGroup;
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
