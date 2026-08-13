package org.bitstrings.idea.plugins.testinsanity.lang;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;

public class JavaTestElementAdapter
    implements TestElementAdapter
{
    @Override
    public boolean isMethod(PsiElement element)
    {
        return (element instanceof PsiMethod);
    }

    @Override
    public boolean isClass(PsiElement element)
    {
        return (element instanceof PsiClass);
    }

    @Override
    public PsiMethod asMethod(PsiElement element)
    {
        return (element instanceof PsiMethod) ? (PsiMethod) element : null;
    }

    @Override
    public PsiClass asClass(PsiElement element, GlobalSearchScope searchScope)
    {
        return (element instanceof PsiClass) ? (PsiClass) element : null;
    }
}
