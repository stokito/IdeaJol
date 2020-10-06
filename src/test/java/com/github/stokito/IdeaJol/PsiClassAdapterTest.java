package com.github.stokito.IdeaJol;

import com.intellij.psi.PsiClass;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.FieldData;

import java.util.List;

public class PsiClassAdapterTest extends LightJavaCodeInsightFixtureTestCase {
    public void testCreateClassDataFromPsiClass() {
        myFixture.configureByText("PackingFields.java", "public class PackingFields {\n" +
                "    boolean bo1, bo2;\n" +
                "    byte b1, b2;\n" +
                "    char c1, c2;\n" +
                "    double d1, d2;\n" +
                "    float f1, f2;\n" +
                "    int i1, i2;\n" +
                "    long l1, l2;\n" +
                "    short s1, s2;\n" +
                "}");

        PsiClass psiClass = myFixture.findClass("PackingFields");
        System.out.println(psiClass.getText());
        ClassData classData = PsiClassAdapter.createClassDataFromPsiClass(psiClass);
        assertEquals(16, classData.fields().size());
        assertField(classData, 0, "bo1", "boolean", "PackingFields");
        assertField(classData, 1, "bo2", "boolean", "PackingFields");
        assertField(classData, 2, "b1", "byte", "PackingFields");
        assertField(classData, 3, "b2", "byte", "PackingFields");
        assertField(classData, 4, "c1", "char", "PackingFields");
        assertField(classData, 5, "c2", "char", "PackingFields");
        assertField(classData, 6, "d1", "double", "PackingFields");
        assertField(classData, 7, "d2", "double", "PackingFields");
        assertField(classData, 8, "f1", "float", "PackingFields");
        assertField(classData, 9, "f2", "float", "PackingFields");
        assertField(classData, 10, "i1", "int", "PackingFields");
        assertField(classData, 11, "i2", "int", "PackingFields");
        assertField(classData, 12, "l1", "long", "PackingFields");
        assertField(classData, 13, "l2", "long", "PackingFields");
        assertField(classData, 14, "s1", "short", "PackingFields");
        assertField(classData, 15, "s2", "short", "PackingFields");
    }

    private void assertField(ClassData classData, int index, String name, String typeClass, String hostClass) {
        assertEquals(name, classData.fields().get(index).name());
        assertEquals(typeClass, classData.fields().get(index).typeClass());
        assertEquals(hostClass, classData.fields().get(index).hostClass());
    }

}
