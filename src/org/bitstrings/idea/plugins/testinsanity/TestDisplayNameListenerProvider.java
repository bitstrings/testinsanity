package org.bitstrings.idea.plugins.testinsanity;

import java.util.Objects;

import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.bitstrings.idea.plugins.testinsanity.lang.TestElementAdapters;
import org.bitstrings.idea.plugins.testinsanity.util.TestDisplayNames;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.listeners.RefactoringElementListenerProvider;

public class TestDisplayNameListenerProvider
    implements RefactoringElementListenerProvider
{
    @Override
    public RefactoringElementListener getListener(PsiElement element)
    {
        if (!TestInsanitySettings.getInstance(element.getProject()).isSyncDisplayName())
        {
            return null;
        }

        PsiMethod method = TestElementAdapters.asMethod(element);

        if ((method == null) || (method.getName() == null))
        {
            return null;
        }

        PsiAnnotation displayNameAnnotation = findWritableDisplayName(method);

        if (
            (displayNameAnnotation == null)
                || !Objects.equals(
                    readDisplayName(displayNameAnnotation), TestDisplayNames.humanize(method.getName()))
        )
        {
            return null;
        }

        return new RefactoringElementListener()
        {
            @Override
            public void elementMoved(PsiElement newElement)
            {
            }

            @Override
            public void elementRenamed(PsiElement newElement)
            {
                syncDisplayName(newElement);
            }
        };
    }

    private static void syncDisplayName(PsiElement newElement)
    {
        PsiMethod method = TestElementAdapters.asMethod(newElement);

        if ((method == null) || (method.getName() == null))
        {
            return;
        }

        PsiAnnotation displayNameAnnotation = findWritableDisplayName(method);

        if (displayNameAnnotation == null)
        {
            return;
        }

        PsiAnnotationMemberValue displayNameValue = displayNameAnnotation.findDeclaredAttributeValue(null);

        if (displayNameValue == null)
        {
            return;
        }

        Project project = newElement.getProject();

        String displayName = TestDisplayNames.humanize(method.getName());

        WriteCommandAction
            .runWriteCommandAction(
                project,
                () ->
                {
                    displayNameValue.replace(
                        JavaPsiFacade
                            .getElementFactory(project)
                            .createExpressionFromText(
                                "\"" + StringUtil.escapeStringCharacters(displayName) + "\"", displayNameAnnotation));
                });
    }

    private static PsiAnnotation findWritableDisplayName(PsiMethod method)
    {
        PsiAnnotation displayNameAnnotation = method.getAnnotation(TestDisplayNames.DISPLAY_NAME_FQN);

        return ((displayNameAnnotation == null) || !displayNameAnnotation.isPhysical())
            ? null
            : displayNameAnnotation;
    }

    private static String readDisplayName(PsiAnnotation displayNameAnnotation)
    {
        PsiAnnotationMemberValue displayNameValue = displayNameAnnotation.findDeclaredAttributeValue(null);

        return (displayNameValue instanceof PsiLiteralExpression)
            ? String.valueOf(((PsiLiteralExpression) displayNameValue).getValue())
            : null;
    }
}
