package org.bitstrings.idea.plugins.testinsanity;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bitstrings.idea.plugins.testinsanity.util.TestPatternException;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;

public final class TestSchemes
{
    private final List<TestScheme> schemes;

    public TestSchemes(List<TestScheme> schemes)
    {
        this.schemes = List.copyOf(schemes);
    }

    public List<TestScheme> getSchemes()
    {
        return schemes;
    }

    public void validatePatterns()
        throws TestPatternException
    {
        for (TestScheme scheme : schemes)
        {
            scheme.getClassMediator().validatePattern();
            scheme.getMethodMediator().validatePattern();
        }
    }

    private TestScheme schemeFor(PsiClass testClass)
    {
        if (testClass == null)
        {
            return null;
        }

        for (TestScheme scheme : schemes)
        {
            if (scheme.getClassMediator().resolveTestClass(testClass) != null)
            {
                return scheme;
            }
        }

        return null;
    }

    public List<PsiClass> getTestClasses(PsiClass subjectClass, GlobalSearchScope searchScope)
    {
        Set<PsiClass> testClasses = new LinkedHashSet<>();

        for (TestScheme scheme : schemes)
        {
            testClasses.addAll(scheme.getClassMediator().getTestClasses(subjectClass, searchScope));
        }

        return new ArrayList<>(testClasses);
    }

    public PsiClass getSubjectClass(PsiClass testClass, GlobalSearchScope searchScope)
    {
        for (TestScheme scheme : schemes)
        {
            PsiClass subjectClass = scheme.getClassMediator().getSubjectClass(testClass, searchScope);

            if (subjectClass != null)
            {
                return subjectClass;
            }
        }

        return null;
    }

    public boolean isTestClass(PsiClass candidateTestClass)
    {
        for (TestScheme scheme : schemes)
        {
            if (scheme.getClassMediator().isTestClass(candidateTestClass))
            {
                return true;
            }
        }

        return false;
    }

    public PsiClass resolveTestClass(PsiClass candidateClass)
    {
        for (TestScheme scheme : schemes)
        {
            PsiClass testClass = scheme.getClassMediator().resolveTestClass(candidateClass);

            if (testClass != null)
            {
                return testClass;
            }
        }

        return null;
    }

    public String renameTestClassName(String testName, String oldSubjectName, String newSubjectName)
    {
        for (TestScheme scheme : schemes)
        {
            if (scheme.getClassMediator().matchesTestName(testName, oldSubjectName))
            {
                return scheme.getClassMediator().renameTestName(testName, oldSubjectName, newSubjectName);
            }
        }

        return testName;
    }

    public String renameSubjectClassName(String subjectName, String oldTestName, String newTestName)
    {
        for (TestScheme scheme : schemes)
        {
            if (scheme.getClassMediator().matchesTestName(oldTestName, subjectName))
            {
                return scheme.getClassMediator().renameSubjectName(subjectName, oldTestName, newTestName);
            }
        }

        return newTestName;
    }

    public List<PsiMethod> getTestMethods(PsiMethod subjectMethod, List<PsiClass> testClasses)
    {
        Set<PsiMethod> testMethods = new LinkedHashSet<>();

        for (PsiClass testClass : testClasses)
        {
            TestScheme scheme = schemeFor(testClass);

            if (scheme != null)
            {
                testMethods.addAll(scheme.getMethodMediator().getTestMethods(subjectMethod, singletonList(testClass)));
            }
        }

        return new ArrayList<>(testMethods);
    }

    public List<PsiMethod> getSubjectMethods(PsiClass testClass, PsiMethod testMethod, PsiClass subjectClass)
    {
        TestScheme scheme = schemeFor(testClass);

        return (scheme == null)
            ? emptyList()
            : scheme.getMethodMediator().getSubjectMethods(testMethod, subjectClass);
    }

    public String renameTestMethodName(
        PsiClass testClass, String testName, String oldSubjectName, String newSubjectName
    )
    {
        TestScheme scheme = schemeFor(testClass);

        return (scheme == null)
            ? testName
            : scheme.getMethodMediator().renameTestName(testName, oldSubjectName, newSubjectName);
    }

    public String renameSubjectMethodName(
        PsiClass testClass, String subjectName, String oldTestName, String newTestName
    )
    {
        TestScheme scheme = schemeFor(testClass);

        return (scheme == null)
            ? newTestName
            : scheme.getMethodMediator().renameSubjectName(subjectName, oldTestName, newTestName);
    }

    public boolean checkMethodAnnotation(PsiClass testClass, PsiMethod targetMethod, boolean failOnEmpty)
    {
        TestScheme scheme = schemeOrPrimary(testClass);

        return (scheme != null) && scheme.getMethodMediator().checkMethodAnnotation(targetMethod, failOnEmpty);
    }

    public String generateTestClassName(TestScheme scheme, String subjectName)
    {
        return scheme.getClassMediator().generateTestName(subjectName);
    }

    public String generateTestMethodName(TestScheme scheme, String subjectName)
    {
        return scheme.getMethodMediator().generateTestName(subjectName);
    }

    public String findSubjectClassName(PsiClass testClass)
    {
        TestScheme scheme = schemeFor(testClass);

        return (scheme == null) ? null : scheme.getClassMediator().findSubjectName(testClass.getName());
    }

    public String findSubjectMethodName(PsiClass testClass, String testMethodName)
    {
        TestScheme scheme = schemeFor(testClass);

        return (scheme == null) ? null : scheme.getMethodMediator().findSubjectName(testMethodName);
    }

    public PsiClass testClassOf(TestScheme scheme, List<PsiClass> testClasses)
    {
        for (PsiClass testClass : testClasses)
        {
            if (schemeFor(testClass) == scheme)
            {
                return testClass;
            }
        }

        return null;
    }

    private TestScheme schemeOrPrimary(PsiClass testClass)
    {
        TestScheme scheme = schemeFor(testClass);

        return (scheme == null) ? primaryScheme() : scheme;
    }

    private TestScheme primaryScheme()
    {
        return schemes.isEmpty() ? null : schemes.get(0);
    }
}
