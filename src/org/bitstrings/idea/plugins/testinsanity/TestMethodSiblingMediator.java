package org.bitstrings.idea.plugins.testinsanity;

import java.util.List;

import org.bitstrings.idea.plugins.testinsanity.util.TestPatternException;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

public interface TestMethodSiblingMediator
{
    void validatePattern()
        throws TestPatternException;

    List<PsiMethod> getTestMethods(PsiMethod subjectMethod, List<PsiClass> testClasses);

    List<PsiMethod> getSubjectMethods(PsiMethod testMethod, PsiClass subjectClass);

    String renameTestName(String oldTestName, String oldSubjectName, String newSubjectName);

    String renameSubjectName(String subjectName, String oldTestName, String newTestName);

    boolean checkMethodAnnotation(PsiMethod targetMethod, boolean failOnEmpty);
}
