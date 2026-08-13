package org.bitstrings.idea.plugins.testinsanity.util;

import com.intellij.psi.PsiNamedElement;

public final class TestInsanityUtil
{
    private TestInsanityUtil()
    {
    }

    public static boolean psiNameIsSet(PsiNamedElement element)
    {
        return ((element != null) && (element.getName() != null));
    }
}
