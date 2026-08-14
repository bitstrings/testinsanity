package org.bitstrings.idea.plugins.testinsanity;

import java.util.Collection;
import java.util.List;

import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.testIntegration.TestFinderHelper;

public class TestInsanityPluginTest
    extends TestInsanityFixtureTestCase
{
    public void testPluginIsLoadedAndTestSourceRootExists()
    {
        assertNotNull("plugin service must be reachable", RenameTestService.getInstance(getProject()));

        assertNotNull("test source root must exist", findTestSourceRoot());

        assertEquals(
            List.of("${className}Test"),
            TestInsanitySettings.getInstance(getProject()).resolveTestClassPatterns());
    }

    public void testTestFinderResolvesBothDirections()
        throws Exception
    {
        PsiClass subjectClass = addSubjectClass("Color", "public boolean isDarkColor() { return true; }");
        PsiClass testClass = addTestClass("ColorTest", "@org.junit.Test public void testIsDarkColor() {}");

        Collection<PsiElement> tests = TestFinderHelper.findTestsForClass(subjectClass);
        Collection<PsiElement> subjects = TestFinderHelper.findClassesForTest(testClass);

        assertTrue("Navigate to test must find the test class, got " + tests, tests.contains(testClass));
        assertTrue("Navigate to subject must find the subject class, got " + subjects, subjects.contains(subjectClass));
    }

    public void testServiceResolvesSiblingsBothDirections()
        throws Exception
    {
        PsiClass subjectClass = addSubjectClass("Color", "public boolean isDarkColor() { return true; }");
        PsiClass testClass = addTestClass("ColorTest", "@org.junit.Test public void testIsDarkColor() {}");

        RenameTestService renameTestService = RenameTestService.getInstance(getProject());

        assertEquals(List.of(testClass), renameTestService.findTestClasses(subjectClass));
        assertEquals(subjectClass, renameTestService.findSubjectClass(testClass));

        assertEquals(
            List.of(testClass.findMethodsByName("testIsDarkColor", false)[0]),
            renameTestService
                .getTestSchemes()
                .getTestMethods(subjectClass.findMethodsByName("isDarkColor", false)[0], List.of(testClass)));
    }

    public void testRenamingASubjectClassProposesTheTestClassRename()
        throws Exception
    {
        PsiClass subjectClass = addSubjectClass("Color", "public boolean isDarkColor() { return true; }");
        PsiClass testClass = addTestClass("ColorTest", "@org.junit.Test public void testIsDarkColor() {}");

        assertEquals(
            "ColourTest",
            RenameTestService.getInstance(getProject()).renameSubjectClassMapping(subjectClass, "Colour")
                .get(testClass));
    }

    public void testRenamingASubjectMethodProposesTheTestMethodRename()
        throws Exception
    {
        PsiClass subjectClass = addSubjectClass("Color", "public boolean isDarkColor() { return true; }");
        PsiClass testClass = addTestClass("ColorTest", "@org.junit.Test public void testIsDarkColor() {}");

        assertEquals(
            "testIsPaleColor",
            RenameTestService
                .getInstance(getProject())
                .renameSubjectMethodMapping(subjectClass.findMethodsByName("isDarkColor", false)[0], "isPaleColor")
                .get(testClass.findMethodsByName("testIsDarkColor", false)[0]));
    }

    public void testComposedTestAnnotationIsRecognized()
        throws Exception
    {
        addTestSourceFile(
            "IntegrationTest.java",
            "package acme;\n@org.junit.Test\npublic @interface IntegrationTest {}\n");

        PsiClass subjectClass = addSubjectClass("Color", "public boolean isDarkColor() { return true; }");
        PsiClass testClass = addTestClass("ColorTest", "@acme.IntegrationTest public void testIsDarkColor() {}");

        assertEquals(
            List.of(testClass.findMethodsByName("testIsDarkColor", false)[0]),
            RenameTestService
                .getInstance(getProject())
                .getTestSchemes()
                .getTestMethods(subjectClass.findMethodsByName("isDarkColor", false)[0], List.of(testClass)));
    }
}
