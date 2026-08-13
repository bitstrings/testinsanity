package org.bitstrings.idea.plugins.testinsanity.navigation;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.Collection;

import org.bitstrings.idea.plugins.testinsanity.RenameTestService;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanityConfiguration;
import org.bitstrings.idea.plugins.testinsanity.lang.TestElementAdapters;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testIntegration.TestFinder;

public class TestInsanityTestFinder
    implements TestFinder
{
    @Override
    public PsiElement findSourceElement(PsiElement from)
    {
        return findEnabledClass(from);
    }

    @Override
    public Collection<PsiElement> findTestsForClass(PsiElement element)
    {
        PsiClass elementClass = findEnabledClass(element);

        if (elementClass == null)
        {
            return emptyList();
        }

        RenameTestService renameTestService = RenameTestService.getInstance(element.getProject());

        if (renameTestService.getTestClassSiblingMediator().resolveTestClass(elementClass) != null)
        {
            return emptyList();
        }

        return new ArrayList<>(renameTestService.findTestClasses(elementClass));
    }

    @Override
    public Collection<PsiElement> findClassesForTest(PsiElement element)
    {
        PsiClass subjectClass = findSubjectClass(element);

        return (subjectClass == null)
            ? emptyList()
            : singletonList(subjectClass);
    }

    @Override
    public boolean isTest(PsiElement element)
    {
        PsiClass elementClass = findEnabledClass(element);

        return (elementClass != null)
            && (RenameTestService
                .getInstance(element.getProject())
                .getTestClassSiblingMediator()
                .resolveTestClass(elementClass) != null);
    }

    private static PsiClass findSubjectClass(PsiElement element)
    {
        PsiClass elementClass = findEnabledClass(element);

        if (elementClass == null)
        {
            return null;
        }

        RenameTestService renameTestService = RenameTestService.getInstance(element.getProject());

        PsiClass testClass = renameTestService.getTestClassSiblingMediator().resolveTestClass(elementClass);

        return (testClass == null)
            ? null
            : renameTestService.findSubjectClass(testClass);
    }

    private static PsiClass findEnabledClass(PsiElement element)
    {
        return TestInsanityConfiguration.getInstance(element.getProject()).isNavigationEnabled()
            ? findContainingClass(element)
            : null;
    }

    private static PsiClass findContainingClass(PsiElement element)
    {
        return TestElementAdapters
            .asClass(
                PsiTreeUtil.findFirstParent(element, TestElementAdapters::isClass), element.getResolveScope());
    }
}
