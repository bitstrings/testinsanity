package org.bitstrings.idea.plugins.testinsanity.codeinsight;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.ParameterInfoUtils;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMethod;
import com.intellij.psi.PsiAnnotationParameterList;

public class TestInlayProvider
    implements ParameterInfoHandler<PsiAnnotationParameterList, PsiAnnotationMethod>
{
    @Override
    public boolean couldShowInLookup()
    {
        return false;
    }

    @Nullable
    @Override
    public Object[] getParametersForLookup(LookupElement lookupElement, ParameterInfoContext parameterInfoContext)
    {
        return null;
    }

    @Nullable
    @Override
    public PsiAnnotationParameterList findElementForParameterInfo(CreateParameterInfoContext context)
    {
        PsiAnnotation annotation =
            ParameterInfoUtils.findParentOfType(context.getFile(), context.getOffset(), PsiAnnotation.class);

        if (annotation != null)
        {
            return annotation.getParameterList();
        }

        return null;
    }

    @Override
    public void showParameterInfo(
        @NotNull PsiAnnotationParameterList psiAnnotationParameterList,
        @NotNull CreateParameterInfoContext context
    )
    {
        context.showHint(psiAnnotationParameterList, psiAnnotationParameterList.getTextRange().getStartOffset() + 1, this);
    }

    @Nullable
    @Override
    public PsiAnnotationParameterList findElementForUpdatingParameterInfo(
        @NotNull UpdateParameterInfoContext context
    )
    {
        final PsiAnnotation annotation = ParameterInfoUtils.findParentOfType(context.getFile(), context.getOffset(), PsiAnnotation.class);
        return annotation != null ? annotation.getParameterList() : null;
    }

    @Override
    public void updateParameterInfo(
        @NotNull PsiAnnotationParameterList psiAnnotationParameterList,
        @NotNull UpdateParameterInfoContext context
    )
    {
    }

    @Override
    public void updateUI(
        PsiAnnotationMethod psiAnnotationMethod,
        @NotNull ParameterInfoUIContext context
    )
    {
        context.setupUIComponentPresentation("YES", 0, 3, false, psiAnnotationMethod.isDeprecated(), false, context.getDefaultParameterColor());
    }
}
