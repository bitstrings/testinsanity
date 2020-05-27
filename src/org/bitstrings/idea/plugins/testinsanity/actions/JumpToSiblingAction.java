package org.bitstrings.idea.plugins.testinsanity.actions;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.bitstrings.idea.plugins.testinsanity.util.TestInsanityUtil.getLightClassMethod;

import java.util.List;

import org.bitstrings.idea.plugins.testinsanity.RenameTestService;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.bitstrings.idea.plugins.testinsanity.util.TestInsanityUtil;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.navigation.GotoTargetHandler;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.scope.ProjectProductionScope;
import com.intellij.psi.search.scope.TestsScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.serviceContainer.NonInjectable;

public class JumpToSiblingAction
    extends BaseCodeInsightAction
{
    private final PsiElement element;

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
            return null;
        }
        @Override
        protected GotoData getSourceAndTargetElements(Editor editor, PsiFile file)
        {
            Project project = file.getProject();

            PsiElement element =
                JumpToSiblingAction.this.element == null
                    ? file.findElementAt(editor.getCaretModel().getOffset())
                    : JumpToSiblingAction.this.element;

            RenameTestService renameTestService = RenameTestService.getInstance(project);

            PsiElement elementParent =
                PsiTreeUtil.findFirstParent(
                    element, parent -> (parent instanceof PsiMethod) || (parent instanceof KtNamedFunction)
                );

            PsiMethod elementMethod =
                elementParent instanceof KtNamedFunction
                    ? getLightClassMethod((KtNamedFunction) elementParent)
                    : (PsiMethod) elementParent;

            PsiClass elementClass;

            if (elementParent == null)
            {
                elementParent = PsiTreeUtil
                    .findFirstParent(element, parent -> (parent instanceof PsiClass) || (parent instanceof KtClass));

                if (elementParent instanceof KtClass)
                {
                    elementClass = JavaPsiFacade.getInstance(project)
                        .findClass(((KtClass) elementParent).getFqName().asString(), element.getResolveScope());
                }
                else
                {
                    elementClass = (PsiClass) elementParent;
                }
            }
            else
            {
                elementClass = elementMethod.getContainingClass();
            }

            if (!TestInsanityUtil.psiNameIsSet(elementClass))
            {
                return null;
            }

            List<PsiMethod> gotoMethods = null;
            List<PsiClass> gotoClasses;

            if (renameTestService.getTestClassSiblingMediator().isTestClass(elementClass))
            {
                PsiClass gotoClass =
                    renameTestService
                        .getTestClassSiblingMediator()
                        .getSubjectClass(
                            elementClass,
                            renameTestService.getSearchScope(elementClass, ProjectProductionScope.INSTANCE)
                        );

                if (gotoClass == null)
                {
                    return null;
                }

                gotoClasses = singletonList(gotoClass);

                if (elementMethod != null)
                {
                    gotoMethods =
                        renameTestService.getTestMethodSiblingMediator().getSubjectMethods(elementMethod, gotoClass);

                    if (gotoMethods.isEmpty())
                    {
                        gotoMethods = null;
                    }
                }
            }
            else
            {
                gotoClasses =
                    renameTestService
                        .getTestClassSiblingMediator()
                        .getTestClasses(
                            elementClass,
                            renameTestService.getSearchScope(elementClass, TestsScope.INSTANCE)
                        );

                if (gotoClasses.isEmpty())
                {
                    return null;
                }

                if (elementMethod != null)
                {
                    gotoMethods =
                        renameTestService.getTestMethodSiblingMediator().getTestMethods(elementMethod, gotoClasses);

                    if (gotoMethods.isEmpty())
                    {
                        gotoMethods = null;
                    }
                }
            }

            return gotoMethods == null
                ? new GotoData(file, gotoClasses.toArray(new PsiElement[gotoClasses.size()]), emptyList())
                : new GotoData(file, gotoMethods.toArray(new PsiElement[gotoMethods.size()]), emptyList());
        }
    }

    public JumpToSiblingAction()
    {
        this(null);
    }

    @NonInjectable
    public JumpToSiblingAction(PsiElement element)
    {
        this.element = element;
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

        presentation.setVisible(TestInsanitySettings.getInstance(project).isNavigationEnabled());

        presentation.setEnabled(false);

        PsiElement element =
            this.element == null
                ? file.findElementAt(editor.getCaretModel().getOffset())
                : this.element;

        PsiElement elementParent =
            PsiTreeUtil.findFirstParent(
                element, parent -> (parent instanceof PsiClass) || (parent instanceof KtClass));

        if (elementParent instanceof KtClass)
        {
            elementParent = JavaPsiFacade.getInstance(project)
                .findClass(((KtClass) elementParent).getFqName().asString(), element.getResolveScope());
        }

        if (elementParent == null)
        {
            return;
        }

        PsiClass elementClass = (PsiClass) elementParent;

        if (!TestInsanityUtil.psiNameIsSet(elementClass))
        {
            return;
        }

        RenameTestService renameTestService = RenameTestService.getInstance(project);

        if (renameTestService.getTestClassSiblingMediator().isTestClass(elementClass))
        {
            presentation.setText("_Jump to Subject", true);
        }
        else
        {
            presentation.setText("_Jump to Test", true);
        }

        presentation.setEnabled(true);
    }
}
