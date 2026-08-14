package org.bitstrings.idea.plugins.testinsanity.intentions;

import org.bitstrings.idea.plugins.testinsanity.TestInsanityFixtureTestCase;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;

public class CreateTestSiblingIntentionTest
    extends TestInsanityFixtureTestCase
{
    private final CreateTestSiblingIntention intention = new CreateTestSiblingIntention();

    public void testIsAvailable_subjectWithoutAnyTestClass_namesThePrimarySchemeClass()
    {
        useSchemes(
            scheme("unit", "${className}Test", "test${subjectName}*"),
            scheme("it", "${className}IT", "${subjectName}_+"));

        PsiClass subjectClass = addSubjectClass("Color", "public boolean isDarkColor() { return true; }");

        assertTrue(intention.isAvailable(getProject(), null, nameOf(subjectClass)));
        assertEquals("Create test sibling...", intention.getText());
    }

    public void testIsAvailable_anotherSchemeAlreadyHasItsTestClass_stillOffersTheMissingOne()
        throws Exception
    {
        useSchemes(
            scheme("unit", "${className}Test", "test${subjectName}*"),
            scheme("it", "${className}IT", "${subjectName}_+"));

        PsiClass subjectClass = addSubjectClass("Color", "public boolean isDarkColor() { return true; }");

        addTestClass("ColorIT", "@org.junit.Test public void isDarkColor_test() {}");

        assertTrue(intention.isAvailable(getProject(), null, nameOf(subjectClass)));
        assertEquals("Create test class ColorTest", intention.getText());
    }

    public void testIsAvailable_everySchemeHasItsTestClass_isNotOffered()
        throws Exception
    {
        useSchemes(
            scheme("unit", "${className}Test", "test${subjectName}*"),
            scheme("it", "${className}IT", "${subjectName}_+"));

        PsiClass subjectClass = addSubjectClass("Color", "public boolean isDarkColor() { return true; }");

        addTestClass("ColorTest", "@org.junit.Test public void testIsDarkColor() {}");
        addTestClass("ColorIT", "@org.junit.Test public void isDarkColor_test() {}");

        assertFalse(intention.isAvailable(getProject(), null, nameOf(subjectClass)));
    }

    public void testInvoke_withoutAnEditor_createsEveryMissingTestClass()
    {
        useSchemes(
            scheme("unit", "${className}Test", "test${subjectName}*"),
            scheme("it", "${className}IT", "${subjectName}_+"));

        PsiClass subjectClass = addSubjectClass("Color", "public boolean isDarkColor() { return true; }");

        intention.invoke(getProject(), null, nameOf(subjectClass));

        assertNotNull("the unit scheme class was not created", findClass("ColorTest"));
        assertNotNull("the integration scheme class was not created", findClass("ColorIT"));
    }

    public void testIsAvailable_testClassWithoutTheTestMethod_namesTheMethodAndItsClass()
        throws Exception
    {
        useSchemes(scheme("unit", "${className}Test", "test${subjectName}*"));

        PsiClass subjectClass = addSubjectClass("Color", "public boolean isDarkColor() { return true; }");

        addTestClass("ColorTest", "");

        assertTrue(
            intention
                .isAvailable(
                    getProject(), null, nameOf(subjectClass.findMethodsByName("isDarkColor", false)[0])));
        assertEquals("Create test method testIsDarkColor in ColorTest", intention.getText());
    }

    public void testIsAvailable_subjectInsideTestSources_isNotOffered()
        throws Exception
    {
        useSchemes(scheme("unit", "${className}Test", "test${subjectName}*"));

        PsiClass testClass = addTestClass("ColorTest", "@org.junit.Test public void testIsDarkColor() {}");

        assertFalse(intention.isAvailable(getProject(), null, nameOf(testClass)));
    }

    private PsiClass findClass(String className)
    {
        return JavaPsiFacade
            .getInstance(getProject())
            .findClass(className, GlobalSearchScope.allScope(getProject()));
    }

    private static PsiElement nameOf(PsiClass psiClass)
    {
        return psiClass.getNameIdentifier();
    }

    private static PsiElement nameOf(com.intellij.psi.PsiMethod psiMethod)
    {
        return psiMethod.getNameIdentifier();
    }
}
