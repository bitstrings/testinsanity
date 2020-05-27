package org.bitstrings.idea.plugins.testinsanity;

import java.util.List;

import org.bitstrings.idea.plugins.testinsanity.util.TestPatternException;

import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;

public interface TestClassSiblingMediator
{
    void validatePattern()
        throws TestPatternException;

    List<PsiClass> getTestClasses(PsiClass subjectClass, GlobalSearchScope searchScope);

    PsiClass getSubjectClass(PsiClass testClass, GlobalSearchScope searchScope);

    String renameTestName(String testName, String oldSubjectName, String newSubjectName);

    String renameSubjectName(String subjectName, String oldTestName, String newTestName);

    boolean isTestClass(PsiClass candidateTestClass);
}
