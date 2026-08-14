package org.bitstrings.idea.plugins.testinsanity.actions;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.List;

import org.bitstrings.idea.plugins.testinsanity.RenameTestService;
import org.bitstrings.idea.plugins.testinsanity.TestInsanityBundle;
import org.bitstrings.idea.plugins.testinsanity.TestSchemes;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanityConfiguration;
import org.bitstrings.idea.plugins.testinsanity.lang.TestElementAdapters;
import org.bitstrings.idea.plugins.testinsanity.util.TestInsanityUtil;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.navigation.GotoTargetHandler;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

public class JumpToSiblingAction
    extends BaseCodeInsightAction
{
    protected class MyGotoTargetHandler
        extends GotoTargetHandler
    {
        @Override
        protected String getFeatureUsedKey()
        {
            return MyGotoTargetHandler.class.getName();
        }

        @Override
        protected String getNotFoundMessage(Project project, Editor editor, PsiFile file)
        {
            return TestInsanityBundle.message("testinsanity.action.jump.notfound", file.getName());
        }

        @Override
        protected GotoData getSourceAndTargetElements(Editor editor, PsiFile file)
        {
            PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());

            if (element == null)
            {
                return null;
            }

            RenameTestService renameTestService = RenameTestService.getInstance(file.getProject());

            PsiMethod elementMethod =
                TestElementAdapters.asMethod(PsiTreeUtil.findFirstParent(element, TestElementAdapters::isMethod));

            PsiClass elementClass =
                (elementMethod == null)
                    ? TestElementAdapters.asClass(
                        PsiTreeUtil.findFirstParent(element, TestElementAdapters::isClass),
                        element.getResolveScope()
                    )
                    : elementMethod.getContainingClass();

            if (!TestInsanityUtil.psiNameIsSet(elementClass))
            {
                return null;
            }

            TestSchemes schemes = renameTestService.getTestSchemes();

            PsiClass testClass = schemes.resolveTestClass(elementClass);

            List<PsiMethod> gotoMethods = null;
            List<PsiClass> gotoClasses;

            if (testClass == null)
            {
                gotoClasses = renameTestService.findTestClasses(elementClass);

                if (gotoClasses.isEmpty())
                {
                    return null;
                }

                if (elementMethod != null)
                {
                    gotoMethods = nullIfEmpty(schemes.getTestMethods(elementMethod, gotoClasses));
                }
            }
            else
            {
                PsiClass gotoClass = renameTestService.findSubjectClass(testClass);

                if (gotoClass == null)
                {
                    return null;
                }

                gotoClasses = singletonList(gotoClass);

                if (elementMethod != null)
                {
                    gotoMethods =
                        nullIfEmpty(schemes.getSubjectMethods(testClass, elementMethod, gotoClass));
                }
            }

            return gotoMethods == null
                ? new GotoData(file, gotoClasses.toArray(PsiElement.EMPTY_ARRAY), emptyList())
                : new GotoData(file, gotoMethods.toArray(PsiElement.EMPTY_ARRAY), emptyList());
        }

        private <T> List<T> nullIfEmpty(List<T> elements)
        {
            return elements.isEmpty() ? null : elements;
        }
    }

    @Override
    protected CodeInsightActionHandler getHandler()
    {
        return new MyGotoTargetHandler();
    }

    @Override
    protected void update(Presentation presentation, Project project, Editor editor, PsiFile file)
    {
        super.update(presentation, project, editor, file);

        presentation.setVisible(TestInsanityConfiguration.getInstance(project).isNavigationEnabled());

        presentation.setEnabled(false);

        PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());

        if (element == null)
        {
            return;
        }

        PsiClass elementClass =
            TestElementAdapters
                .asClass(
                    PsiTreeUtil.findFirstParent(element, TestElementAdapters::isClass), element.getResolveScope());

        if (!TestInsanityUtil.psiNameIsSet(elementClass))
        {
            return;
        }

        presentation
            .setText(
                TestInsanityBundle
                    .message(
                        (RenameTestService
                            .getInstance(project)
                            .getTestSchemes()
                            .resolveTestClass(elementClass) == null)
                                ? "testinsanity.action.jump.test"
                                : "testinsanity.action.jump.subject"),
                true);

        presentation.setEnabled(true);
    }
}
