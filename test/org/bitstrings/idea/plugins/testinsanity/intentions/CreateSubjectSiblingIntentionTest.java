package org.bitstrings.idea.plugins.testinsanity.intentions;

import org.bitstrings.idea.plugins.testinsanity.TestInsanityFixtureTestCase;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;

public class CreateSubjectSiblingIntentionTest
    extends TestInsanityFixtureTestCase
{
    private final CreateSubjectSiblingIntention intention = new CreateSubjectSiblingIntention();

    public void testIsAvailable_testClassWithoutASubject_namesTheSubjectClass()
        throws Exception
    {
        useSchemes(scheme("it", "${className}IT", "${subjectName}_+"));

        PsiClass testClass = addTestClass("ColorIT", "@org.junit.Test public void isDarkColor_test() {}");

        assertTrue(intention.isAvailable(getProject(), null, nameOf(testClass)));
        assertEquals("Create subject class Color", intention.getText());
    }

    public void testInvoke_testClassWithoutASubject_createsTheSubjectClass()
        throws Exception
    {
        useSchemes(scheme("it", "${className}IT", "${subjectName}_+"));

        PsiClass testClass = addTestClass("ColorIT", "@org.junit.Test public void isDarkColor_test() {}");

        intention.invoke(getProject(), null, nameOf(testClass));

        assertNotNull("the subject class was not created", findClass("Color"));
    }

    public void testIsAvailable_subjectAlreadyExists_isNotOffered()
        throws Exception
    {
        useSchemes(scheme("it", "${className}IT", "${subjectName}_+"));

        addSubjectClass("Color", "public boolean isDarkColor() { return true; }");

        PsiClass testClass = addTestClass("ColorIT", "@org.junit.Test public void isDarkColor_test() {}");

        assertFalse(intention.isAvailable(getProject(), null, nameOf(testClass)));
    }

    public void testIsAvailable_subjectClassInProductionSources_isNotOffered()
    {
        useSchemes(scheme("it", "${className}IT", "${subjectName}_+"));

        PsiClass subjectClass = addSubjectClass("Color", "public boolean isDarkColor() { return true; }");

        assertFalse(intention.isAvailable(getProject(), null, nameOf(subjectClass)));
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
}
