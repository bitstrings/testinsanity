package org.bitstrings.idea.plugins.testinsanity;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bitstrings.idea.plugins.testinsanity.util.TestPatternException;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

public class CompositeTestMethodSiblingMediator
    implements TestMethodSiblingMediator
{
    private final List<TestMethodSiblingMediator> mediators;

    public CompositeTestMethodSiblingMediator(List<TestMethodSiblingMediator> mediators)
    {
        this.mediators = List.copyOf(mediators);
    }

    @Override
    public void validatePattern()
        throws TestPatternException
    {
        for (TestMethodSiblingMediator mediator : mediators)
        {
            mediator.validatePattern();
        }
    }

    @Override
    public List<PsiMethod> getTestMethods(PsiMethod subjectMethod, List<PsiClass> testClasses)
    {
        Set<PsiMethod> testMethods = new LinkedHashSet<>();

        for (TestMethodSiblingMediator mediator : mediators)
        {
            testMethods.addAll(mediator.getTestMethods(subjectMethod, testClasses));
        }

        return new ArrayList<>(testMethods);
    }

    @Override
    public List<PsiMethod> getSubjectMethods(PsiMethod testMethod, PsiClass subjectClass)
    {
        for (TestMethodSiblingMediator mediator : mediators)
        {
            List<PsiMethod> subjectMethods = mediator.getSubjectMethods(testMethod, subjectClass);

            if (!subjectMethods.isEmpty())
            {
                return subjectMethods;
            }
        }

        return emptyList();
    }

    @Override
    public String renameTestName(String oldTestName, String oldSubjectName, String newSubjectName)
    {
        TestMethodSiblingMediator mediator = findMatching(oldTestName, oldSubjectName);

        return (mediator == null)
            ? oldTestName
            : mediator.renameTestName(oldTestName, oldSubjectName, newSubjectName);
    }

    @Override
    public String renameSubjectName(String subjectName, String oldTestName, String newTestName)
    {
        TestMethodSiblingMediator mediator = findMatching(oldTestName, subjectName);

        return (mediator == null)
            ? newTestName
            : mediator.renameSubjectName(subjectName, oldTestName, newTestName);
    }

    @Override
    public boolean checkMethodAnnotation(PsiMethod targetMethod, boolean failOnEmpty)
    {
        for (TestMethodSiblingMediator mediator : mediators)
        {
            if (mediator.checkMethodAnnotation(targetMethod, failOnEmpty))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean matchesTestName(String testName, String subjectName)
    {
        return (findMatching(testName, subjectName) != null);
    }

    @Override
    public String generateTestName(String subjectName)
    {
        return mediators.isEmpty()
            ? subjectName
            : mediators.get(0).generateTestName(subjectName);
    }

    @Override
    public String findSubjectName(String testName)
    {
        for (TestMethodSiblingMediator mediator : mediators)
        {
            String subjectName = mediator.findSubjectName(testName);

            if (subjectName != null)
            {
                return subjectName;
            }
        }

        return null;
    }

    private TestMethodSiblingMediator findMatching(String testName, String subjectName)
    {
        for (TestMethodSiblingMediator mediator : mediators)
        {
            if (mediator.matchesTestName(testName, subjectName))
            {
                return mediator;
            }
        }

        return null;
    }
}
