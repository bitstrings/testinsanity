package org.bitstrings.idea.plugins.testinsanity;

import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

public class RenameTestServiceTest
    extends TestInsanityFixtureTestCase
{
    public void testRenameSubjectMethodMapping_longerSubjectSibling_leavesItsTestAlone()
        throws Exception
    {
        useSchemes(scheme("default", "${className}Test", "test${subjectName}*"));

        PsiClass subjectClass = addSubjectClass("Sample", "void abc() {}\nvoid abcd() {}");

        addTestClass("SampleTest", testMethods("testAbc", "testAbcd"));

        Map<PsiMethod, String> renames = renamesFor(subjectClass, "abc", "abcx");

        assertEquals(Map.of("testAbc", "testAbcx"), byName(renames));
    }

    public void testRenameSubjectMethodMapping_shorterSubjectSibling_leavesItsTestAlone()
        throws Exception
    {
        useSchemes(scheme("default", "${className}Test", "test${subjectName}*"));

        PsiClass subjectClass = addSubjectClass("Sample", "void ab() {}\nvoid abc() {}");

        addTestClass("SampleTest", testMethods("testAb", "testAbc"));

        Map<PsiMethod, String> renames = renamesFor(subjectClass, "ab", "abx");

        assertEquals(Map.of("testAb", "testAbx"), byName(renames));
    }

    public void testRenameSubjectMethodMapping_noCompetingSibling_renamesTheSuffixedTest()
        throws Exception
    {
        useSchemes(scheme("default", "${className}Test", "test${subjectName}*"));

        PsiClass subjectClass = addSubjectClass("Sample", "void abc() {}");

        addTestClass("SampleTest", testMethods("testAbc", "testAbcd"));

        Map<PsiMethod, String> renames = renamesFor(subjectClass, "abc", "abcx");

        assertEquals(Map.of("testAbc", "testAbcx", "testAbcd", "testAbcxd"), byName(renames));
    }

    public void testRenameSubjectMethodMapping_exactPattern_matchesOnlyTheIdenticalName()
        throws Exception
    {
        useSchemes(scheme("default", "${className}Test", "${subjectName}"));

        PsiClass subjectClass = addSubjectClass("Sample", "void abc() {}\nvoid abcd() {}");

        addTestClass("SampleTest", testMethods("abc", "abcd"));

        Map<PsiMethod, String> renames = renamesFor(subjectClass, "abc", "abcx");

        assertEquals(Map.of("abc", "abcx"), byName(renames));
    }

    public void testGetSubjectMethods_longerSubjectSibling_resolvesToTheLongerSubject()
        throws Exception
    {
        useSchemes(scheme("default", "${className}Test", "test${subjectName}*"));

        PsiClass subjectClass = addSubjectClass("Sample", "void abc() {}\nvoid abcd() {}");

        PsiClass testClass = addTestClass("SampleTest", testMethods("testAbc", "testAbcd"));

        List<PsiMethod> subjectMethods =
            RenameTestService
                .getInstance(getProject())
                .getTestSchemes()
                .getSubjectMethods(testClass, testClass.findMethodsByName("testAbcd", false)[0], subjectClass);

        assertEquals(1, subjectMethods.size());
        assertEquals("abcd", subjectMethods.get(0).getName());
    }

    private Map<PsiMethod, String> renamesFor(PsiClass subjectClass, String subjectName, String newSubjectName)
    {
        return RenameTestService
            .getInstance(getProject())
            .renameSubjectMethodMapping(subjectClass.findMethodsByName(subjectName, false)[0], newSubjectName);
    }

    private static Map<String, String> byName(Map<PsiMethod, String> renames)
    {
        return renames
            .entrySet()
            .stream()
            .collect(toMap(entry -> entry.getKey().getName(), Map.Entry::getValue));
    }

    private static String testMethods(String... methodNames)
    {
        StringBuilder body = new StringBuilder();

        for (String methodName : methodNames)
        {
            body.append("@org.junit.Test\nvoid ").append(methodName).append("() {}\n");
        }

        return body.toString();
    }
}
