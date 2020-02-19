package org.bitstrings.idea.plugins.testinsanity.annotators;

import java.util.List;

import javax.swing.Icon;

import org.bitstrings.idea.plugins.testinsanity.RenameTestService;
import org.bitstrings.idea.plugins.testinsanity.actions.JumpToSiblingAction;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.jetbrains.kotlin.asJava.LightClassUtil;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.scope.ProjectFilesScope;

public class TestAnnotator
    implements Annotator
{
    private static final Icon ICON = IconLoader.getIcon("/icons/gutter_icon.svg");
    private static final Icon ICON_ORPHAN = IconLoader.getIcon("/icons/gutter_orphan_icon.svg");

    private static final String NOT_LINKED_TO_SUBJECT_MESSAGE = "Test not Linked to Subject";
    private static final String NO_SUBJECT_CLASS_MESSAGE = "No Test Subject Class";

    private static class TestMatchGutterIconRenderer
        extends GutterIconRenderer
    {
        private final Icon icon;
        private final int hashCode;
        private final PsiElement element;
        private final String tooltip;

        public TestMatchGutterIconRenderer(Icon icon, PsiElement element, String tooltip)
        {
            this.icon = icon;
            this.hashCode = getIcon().hashCode();
            this.element = element;
            this.tooltip = tooltip;
        }

        @Override
        public String getTooltipText()
        {
            return tooltip;
        }

        @Override
        public AnAction getClickAction()
        {
            return new JumpToSiblingAction(element);
        }

        @Override
        public boolean equals(Object o)
        {
            return (o instanceof TestMatchGutterIconRenderer);
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        @Override
        public Icon getIcon()
        {
            return icon;
        }
    }

    @Override
    public void annotate(PsiElement element, AnnotationHolder annotationHolder)
    {
        Project project = element.getProject();

        if (!TestInsanitySettings.getInstance(project).isGutterAnnotationEnabled())
        {
            return;
        }

        RenameTestService renameTestService = RenameTestService.getInstance(project);

        if (element instanceof KtNamedFunction)
        {
            element = LightClassUtil.INSTANCE.getLightClassMethod((KtNamedFunction) element);
        }

        if (
            !(element instanceof PsiMethod)
                || !renameTestService.getTestMethodSiblingMediator().checkMethodAnnotation((PsiMethod) element, true)
        )
        {
            return;
        }

        annotateTestMethod((PsiMethod) element, renameTestService, annotationHolder);
    }

    protected void annotateTestMethod(
        PsiMethod testMethod, RenameTestService renameTestService, AnnotationHolder annotationHolder
    )
    {
        PsiClass testClass = (PsiClass) testMethod.getParent();

        if (testClass == null)
        {
            return;
        }

        PsiClass subjectClass =
            renameTestService
                .getTestClassSiblingMediator()
                .getSubjectClass(testClass, renameTestService.getSearchScope(testMethod, ProjectFilesScope.INSTANCE));

        String message;
        String tooltip;
        GutterIconRenderer iconRenderer;

        if (subjectClass != null)
        {
            List<PsiMethod> subjectMethods =
                renameTestService.getTestMethodSiblingMediator().getSubjectMethods(testMethod, subjectClass);

            if (subjectMethods.isEmpty())
            {
                message = NOT_LINKED_TO_SUBJECT_MESSAGE;
                tooltip = message;
                iconRenderer = new TestMatchGutterIconRenderer(ICON_ORPHAN, testMethod, NOT_LINKED_TO_SUBJECT_MESSAGE);
            }
            else
            {
                message =
                    "Linked to Subject: " + subjectClass.getName() + "." + subjectMethods.get(0).getName()
                        + " [" + subjectMethods.size() + " Found]";

               tooltip =
                    "Linked to Subject: <b>" + subjectClass.getName() + "." + subjectMethods.get(0).getName() + "</b>"
                        + " [" + subjectMethods.size() + " Found]";

                iconRenderer = new TestMatchGutterIconRenderer(ICON, testMethod, tooltip);
            }
        }
        else
        {
            message = NO_SUBJECT_CLASS_MESSAGE;
            tooltip = message;
            iconRenderer = new TestMatchGutterIconRenderer(ICON_ORPHAN, testMethod, NO_SUBJECT_CLASS_MESSAGE);
        }

        Annotation annotation = annotationHolder.createInfoAnnotation(testMethod, message);
        annotation.setTooltip(tooltip);
        annotation.setGutterIconRenderer(iconRenderer);
        annotation.setNeedsUpdateOnTyping(true);
    }
}
