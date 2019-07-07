package com.github.stokito.IdeaJol;

import com.intellij.psi.PsiClass;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.FieldData;

import java.util.ArrayList;
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
        List<FieldData> fieldDatas = new ArrayList<>(classData.fields());
        assertEquals(16, fieldDatas.size());
        assertField(fieldDatas, 0, "bo1", "boolean", "PackingFields");
        assertField(fieldDatas, 1, "bo2", "boolean", "PackingFields");
        assertField(fieldDatas, 2, "b1", "byte", "PackingFields");
        assertField(fieldDatas, 3, "b2", "byte", "PackingFields");
        assertField(fieldDatas, 4, "c1", "char", "PackingFields");
        assertField(fieldDatas, 5, "c2", "char", "PackingFields");
        assertField(fieldDatas, 6, "d1", "double", "PackingFields");
        assertField(fieldDatas, 7, "d2", "double", "PackingFields");
        assertField(fieldDatas, 8, "f1", "float", "PackingFields");
        assertField(fieldDatas, 9, "f2", "float", "PackingFields");
        assertField(fieldDatas, 10, "i1", "int", "PackingFields");
        assertField(fieldDatas, 11, "i2", "int", "PackingFields");
        assertField(fieldDatas, 12, "l1", "long", "PackingFields");
        assertField(fieldDatas, 13, "l2", "long", "PackingFields");
        assertField(fieldDatas, 14, "s1", "short", "PackingFields");
        assertField(fieldDatas, 15, "s2", "short", "PackingFields");
    }

    private void assertField(List<FieldData> fieldDatas, int index, String name, String typeClass, String hostClass) {
        assertEquals(name, fieldDatas.get(index).name());
        assertEquals(typeClass, fieldDatas.get(index).typeClass());
        assertEquals(hostClass, fieldDatas.get(index).hostClass());
    }

}
