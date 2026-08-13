package org.bitstrings.idea.plugins.testinsanity.util;

import java.util.Comparator;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiNamedElement;

public final class TestInsanityUtil
{
    public static final Comparator<PsiClass> STABLE_CLASS_ORDER =
        Comparator.comparing(PsiClass::getQualifiedName, Comparator.nullsLast(Comparator.naturalOrder()));

    private TestInsanityUtil()
    {
    }

    public static boolean psiNameIsSet(PsiNamedElement element)
    {
        return ((element != null) && (element.getName() != null));
    }
}
