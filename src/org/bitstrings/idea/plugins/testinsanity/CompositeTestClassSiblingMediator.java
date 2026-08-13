package org.bitstrings.idea.plugins.testinsanity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bitstrings.idea.plugins.testinsanity.util.TestPatternException;

import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;

public class CompositeTestClassSiblingMediator
    implements TestClassSiblingMediator
{
    private final List<TestClassSiblingMediator> mediators;

    public CompositeTestClassSiblingMediator(List<TestClassSiblingMediator> mediators)
    {
        this.mediators = List.copyOf(mediators);
    }

    @Override
    public void validatePattern()
        throws TestPatternException
    {
        for (TestClassSiblingMediator mediator : mediators)
        {
            mediator.validatePattern();
        }
    }

    @Override
    public List<PsiClass> getTestClasses(PsiClass subjectClass, GlobalSearchScope searchScope)
    {
        Set<PsiClass> testClasses = new LinkedHashSet<>();

        for (TestClassSiblingMediator mediator : mediators)
        {
            testClasses.addAll(mediator.getTestClasses(subjectClass, searchScope));
        }

        return new ArrayList<>(testClasses);
    }

    @Override
    public PsiClass getSubjectClass(PsiClass testClass, GlobalSearchScope searchScope)
    {
        for (TestClassSiblingMediator mediator : mediators)
        {
            PsiClass subjectClass = mediator.getSubjectClass(testClass, searchScope);

            if (subjectClass != null)
            {
                return subjectClass;
            }
        }

        return null;
    }

    @Override
    public String renameTestName(String testName, String oldSubjectName, String newSubjectName)
    {
        TestClassSiblingMediator mediator = findMatching(testName, oldSubjectName);

        return (mediator == null)
            ? testName
            : mediator.renameTestName(testName, oldSubjectName, newSubjectName);
    }

    @Override
    public String renameSubjectName(String subjectName, String oldTestName, String newTestName)
    {
        TestClassSiblingMediator mediator = findMatching(oldTestName, subjectName);

        return (mediator == null)
            ? newTestName
            : mediator.renameSubjectName(subjectName, oldTestName, newTestName);
    }

    @Override
    public boolean isTestClass(PsiClass candidateTestClass)
    {
        for (TestClassSiblingMediator mediator : mediators)
        {
            if (mediator.isTestClass(candidateTestClass))
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
    public PsiClass resolveTestClass(PsiClass candidateClass)
    {
        for (TestClassSiblingMediator mediator : mediators)
        {
            PsiClass testClass = mediator.resolveTestClass(candidateClass);

            if (testClass != null)
            {
                return testClass;
            }
        }

        return null;
    }

    @Override
    public String generateTestName(String subjectName)
    {
        return mediators.isEmpty()
            ? subjectName
            : mediators.get(0).generateTestName(subjectName);
    }

    private TestClassSiblingMediator findMatching(String testName, String subjectName)
    {
        for (TestClassSiblingMediator mediator : mediators)
        {
            if (mediator.matchesTestName(testName, subjectName))
            {
                return mediator;
            }
        }

        return null;
    }
}
