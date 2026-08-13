package org.bitstrings.idea.plugins.testinsanity.lang;

import org.jetbrains.kotlin.asJava.LightClassUtil;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;

public class KotlinTestElementAdapter
    implements TestElementAdapter
{
    @Override
    public boolean isMethod(PsiElement element)
    {
        return (element instanceof KtNamedFunction);
    }

    @Override
    public boolean isClass(PsiElement element)
    {
        return (element instanceof KtClass);
    }

    @Override
    public PsiMethod asMethod(PsiElement element)
    {
        return (element instanceof KtNamedFunction)
            ? LightClassUtil.INSTANCE.getLightClassMethod((KtNamedFunction) element)
            : null;
    }

    @Override
    public PsiClass asClass(PsiElement element, GlobalSearchScope searchScope)
    {
        if (!(element instanceof KtClass))
        {
            return null;
        }

        FqName fqName = ((KtClass) element).getFqName();

        return (fqName == null)
            ? null
            : JavaPsiFacade.getInstance(element.getProject()).findClass(fqName.asString(), searchScope);
    }
}
