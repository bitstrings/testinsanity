package org.bitstrings.idea.plugins.testinsanity.annotators;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.bitstrings.idea.plugins.testinsanity.util.KotlinJavaUtil.getLightClassMethod;

import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.testinsanity.RenameTestService;
import org.bitstrings.idea.plugins.testinsanity.actions.JumpToSiblingAction;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.presentation.java.ClassPresentationUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.scope.ProjectFilesScope;

public class TestAnnotator
    implements Annotator
{
    private static final Icon GUTTER_CLASS_ICON = IconLoader.getIcon("/icons/gutter_class_icon.svg");
    private static final Icon GUTTER_CLASS_ORPHAN_ICON = IconLoader.getIcon("/icons/gutter_class_orphan_icon.svg");
    private static final Icon GUTTER_METHOD_ICON = IconLoader.getIcon("/icons/gutter_icon.svg");
    private static final Icon GUTTER_METHOD_ORPHAN_ICON = IconLoader.getIcon("/icons/gutter_orphan_icon.svg");

    private static final String NOT_LINKED_TO_SUBJECT_MESSAGE = "Missing Test Subject Method";
    private static final String NO_SUBJECT_CLASS_MESSAGE = "Missing Test Subject Class";

    private static class TestMatchGutterIconRenderer
        extends NavigationGutterIconRenderer
    {
        private final Icon icon;
        private final int hashCode;
        private final PsiElement element;
        private final String tooltip;

        public TestMatchGutterIconRenderer(
            String popupTitle,
            String emptyText,
            Icon icon, PsiElement element, String tooltip
        )
        {
            super(
                popupTitle,
                emptyText,
                DefaultPsiElementCellRenderer::new,
                NotNullLazyValue.createConstantValue(singletonList(SmartPointerManager.createPointer(element)))
            );

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
            return (element == null ? null : new JumpToSiblingAction(element));
        }

        @Override
        public boolean equals(Object object)
        {
            return (object instanceof TestMatchGutterIconRenderer);
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
        GlobalSearchScope searchScope = renameTestService.getSearchScope(element, ProjectFilesScope.INSTANCE);

        if (element instanceof KtNamedFunction)
        {
            element = getLightClassMethod((KtNamedFunction) element);
        }
        else if (element instanceof KtClass)
        {
            element = JavaPsiFacade
                .getInstance(project)
                .findClass(((KtClass) element).getFqName().asString(), searchScope);
        }

        if (element instanceof PsiClass)
        {
            PsiClass elementClass = (PsiClass) element;

            if (renameTestService.getTestClassSiblingMediator().isTestClassName(elementClass.getName()))
            {
                PsiClass subjectClass =
                    renameTestService
                        .getTestClassSiblingMediator()
                        .getSubjectClass(
                            elementClass, searchScope);

                annotateTestClass(subjectClass, elementClass, annotationHolder);
            }
            else
            {
                List<PsiClass> testClasses =
                    renameTestService
                        .getTestClassSiblingMediator()
                        .getTestClasses(
                            elementClass, searchScope);

                annotateSubjectClass(elementClass, testClasses, annotationHolder);
            }

            return;
        }

        if (element instanceof PsiMethod)
        {
            PsiClass elementClass = ((PsiMethod) element).getContainingClass();

            if (renameTestService.getTestClassSiblingMediator().isTestClassName(elementClass.getName()))
            {
                PsiClass subjectClass =
                    renameTestService
                        .getTestClassSiblingMediator()
                        .getSubjectClass(elementClass, searchScope);

                List<PsiMethod> subjectMethods =
                    subjectClass == null
                        ? emptyList()
                        : renameTestService.getTestMethodSiblingMediator()
                            .getSubjectMethods((PsiMethod) element, subjectClass);

                annotateTestMethod(
                    (PsiMethod) element,
                    (subjectClass == null ? Collections.emptyList() : singletonList(subjectClass)), subjectMethods,
                    "Subject", false,
                    renameTestService, annotationHolder
                );
            }
            else
            {
                List<PsiClass> testClasses =
                    renameTestService
                        .getTestClassSiblingMediator()
                        .getTestClasses(elementClass, searchScope);

                List<PsiMethod> testMethods =
                    renameTestService.getTestMethodSiblingMediator()
                        .getTestMethods((PsiMethod) element, testClasses);

                annotateTestMethod(
                    (PsiMethod) element, testClasses, testMethods,
                    "Test", true,
                    renameTestService, annotationHolder
                );
            }
        }
    }

    protected void annotateSubjectClass(
        PsiClass subjectClass, List<PsiClass> testClasses, AnnotationHolder annotationHolder
    )
    {
        if (testClasses.isEmpty())
        {
            return;
        }

        String message = "Class Tested (Found " + testClasses.size() + ")";
        String tooltip = message;
        GutterIconRenderer iconRenderer =
            new TestMatchGutterIconRenderer(message, message, GUTTER_CLASS_ICON, subjectClass, tooltip);

        createAnnotation(annotationHolder, subjectClass, message, iconRenderer);
    }

    protected void annotateTestClass(
        PsiClass subjectClass, PsiClass testClass, AnnotationHolder annotationHolder
    )
    {
        String message;
        String tooltip;
        GutterIconRenderer iconRenderer;

        if (subjectClass != null)
        {
            String subjectClassPres = ClassPresentationUtil.getNameForClass(subjectClass, true);

            message = "Subject Class [ " + subjectClassPres + " ]";
            tooltip = "Subject Class [ <a href=\"#javaClass/" + subjectClassPres + "\">"
                + getAbbreviatedText(subjectClassPres, 48) + "</a> ]";
            iconRenderer = new TestMatchGutterIconRenderer(message, message, GUTTER_CLASS_ICON, testClass, tooltip);
        }
        else
        {
            message = NO_SUBJECT_CLASS_MESSAGE;
            iconRenderer = new TestMatchGutterIconRenderer(
                message, message, GUTTER_CLASS_ORPHAN_ICON, testClass, NOT_LINKED_TO_SUBJECT_MESSAGE);
        }

        createAnnotation(annotationHolder, testClass, message, iconRenderer);
    }

    protected void annotateTestMethod(
        PsiMethod method, List<PsiClass> siblingClasses, List<PsiMethod> siblingMethods,
        String foundMessageIdentifier, boolean ignoreMissing,
        RenameTestService renameTestService, AnnotationHolder annotationHolder
    )
    {
        String message;
        String tooltip;
        GutterIconRenderer iconRenderer;

        if (!siblingClasses.isEmpty())
        {
            if (siblingMethods.isEmpty())
            {
                if (ignoreMissing)
                {
                    return;
                }

                message = NOT_LINKED_TO_SUBJECT_MESSAGE;
                iconRenderer = new TestMatchGutterIconRenderer(
                    message, message, GUTTER_METHOD_ORPHAN_ICON, method, NOT_LINKED_TO_SUBJECT_MESSAGE);
            }
            else
            {
                String siblingMethodClassPres = ClassPresentationUtil.getContextName(siblingMethods.get(0), true);
                String siblingtMethodNamePres = siblingMethods.get(0).getName();
                String siblingMethodPres =
                    getAbbreviatedText(siblingMethodClassPres + "." + siblingtMethodNamePres, 48);

                message =
                    foundMessageIdentifier
                        + " Method [ " + siblingMethodPres + " ] (" + siblingMethods.size() + " Found)";
                tooltip =
                    foundMessageIdentifier
                        + " Method [ <a href=\"#javaClass/" + siblingMethodClassPres + "\">"
                        + siblingMethodPres
                        + "</a> ]"
                        + " (" + siblingMethods.size() + " Found)";

                iconRenderer = new TestMatchGutterIconRenderer(message, message, GUTTER_METHOD_ICON, method, tooltip);
            }
        }
        else
        {
            if (ignoreMissing)
            {
                return;
            }

            message = NO_SUBJECT_CLASS_MESSAGE;
            iconRenderer =
                new TestMatchGutterIconRenderer(
                    message, message, GUTTER_METHOD_ORPHAN_ICON, method, NO_SUBJECT_CLASS_MESSAGE);
        }

        createAnnotation(annotationHolder, method, message, iconRenderer);
    }

    protected void createAnnotation(
        AnnotationHolder annotationHolder, PsiNamedElement element, String message, GutterIconRenderer iconRenderer
    )
    {
        Annotation annotation =
            annotationHolder.createAnnotation(HighlightSeverity.INFORMATION, element.getTextRange(), message);
        annotation.setGutterIconRenderer(iconRenderer);
        annotation.setNeedsUpdateOnTyping(true);
    }

    private String getAbbreviatedText(String text, int maxLength)
    {
        return StringUtils.abbreviate(text, text.length() - maxLength, maxLength + 4);
    }
}
