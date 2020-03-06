package org.bitstrings.idea.plugins.testinsanity.util;

import org.jetbrains.kotlin.asJava.LightClassUtil;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import com.intellij.psi.PsiMethod;

public final class TestInsanityUtil
{
    private TestInsanityUtil()
    {
    }

    public static PsiMethod getLightClassMethod(KtNamedFunction ktFunction)
    {
        return LightClassUtil.INSTANCE.getLightClassMethod(ktFunction);
    }
}