package org.bitstrings.idea.plugins.testinsanity.lang;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;

public interface TestElementAdapter
{
    boolean isMethod(PsiElement element);

    boolean isClass(PsiElement element);

    PsiMethod asMethod(PsiElement element);

    PsiClass asClass(PsiElement element, GlobalSearchScope searchScope);
}
