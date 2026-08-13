package org.bitstrings.idea.plugins.testinsanity.lang;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;

public final class TestElementAdapters
{
    private static final ExtensionPointName<TestElementAdapter> EP_NAME =
        ExtensionPointName.create("org.bitstrings.idea.plugins.TestInsanity.testElementAdapter");

    private TestElementAdapters()
    {
    }

    public static boolean isMethod(PsiElement element)
    {
        return ((element != null) && (EP_NAME.findFirstSafe(adapter -> adapter.isMethod(element)) != null));
    }

    public static boolean isClass(PsiElement element)
    {
        return ((element != null) && (EP_NAME.findFirstSafe(adapter -> adapter.isClass(element)) != null));
    }

    public static PsiMethod asMethod(PsiElement element)
    {
        return (element == null) ? null : EP_NAME.computeSafeIfAny(adapter -> adapter.asMethod(element));
    }

    public static PsiClass asClass(PsiElement element, GlobalSearchScope searchScope)
    {
        return (element == null) ? null : EP_NAME.computeSafeIfAny(adapter -> adapter.asClass(element, searchScope));
    }
}
