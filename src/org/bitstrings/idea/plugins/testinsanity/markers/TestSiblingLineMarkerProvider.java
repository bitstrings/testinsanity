package org.bitstrings.idea.plugins.testinsanity.markers;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.Collection;
import java.util.List;

import javax.swing.Icon;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.testinsanity.RenameTestService;
import org.bitstrings.idea.plugins.testinsanity.TestInsanityBundle;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.bitstrings.idea.plugins.testinsanity.lang.TestElementAdapters;
import org.bitstrings.idea.plugins.testinsanity.util.TestInsanityUtil;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.presentation.java.ClassPresentationUtil;

public class TestSiblingLineMarkerProvider
    extends RelatedItemLineMarkerProvider
{
    private static final int MAX_FQN_LENGTH = 72;

    private static final String DISPLAY_NAME_FQN = "org.junit.jupiter.api.DisplayName";

    private static final Icon GUTTER_CLASS_ICON =
        IconLoader.getIcon("/icons/gutter_class_icon.svg", TestSiblingLineMarkerProvider.class);
    private static final Icon GUTTER_CLASS_ORPHAN_ICON =
        IconLoader.getIcon("/icons/gutter_class_orphan_icon.svg", TestSiblingLineMarkerProvider.class);
    private static final Icon GUTTER_METHOD_ICON =
        IconLoader.getIcon("/icons/gutter_icon.svg", TestSiblingLineMarkerProvider.class);
    private static final Icon GUTTER_METHOD_ORPHAN_ICON =
        IconLoader.getIcon("/icons/gutter_orphan_icon.svg", TestSiblingLineMarkerProvider.class);

    private static final String NOT_LINKED_TO_SUBJECT_MESSAGE = "Missing Test Subject Method";
    private static final String NO_SUBJECT_CLASS_MESSAGE = "Missing Test Subject Class";

    @Override
    public String getName()
    {
        return TestInsanityBundle.message("testinsanity.marker.name");
    }

    @Override
    public Icon getIcon()
    {
        return GUTTER_CLASS_ICON;
    }

    @Override
    protected void collectNavigationMarkers(
        PsiElement element, Collection<? super RelatedItemLineMarkerInfo<?>> result
    )
    {
        if (element.getFirstChild() != null)
        {
            return;
        }

        PsiElement owner = element.getParent();

        if (
            !(owner instanceof PsiNameIdentifierOwner)
                || (((PsiNameIdentifierOwner) owner).getNameIdentifier() != element)
        )
        {
            return;
        }

        if (!TestInsanitySettings.getInstance(element.getProject()).isGutterAnnotationEnabled())
        {
            return;
        }

        if (TestElementAdapters.isMethod(owner))
        {
            collectMethodMarker(element, owner, result);

            return;
        }

        if (TestElementAdapters.isClass(owner))
        {
            collectClassMarker(element, owner, result);
        }
    }

    protected void collectClassMarker(
        PsiElement anchor, PsiElement owner, Collection<? super RelatedItemLineMarkerInfo<?>> result
    )
    {
        PsiClass ownerClass = TestElementAdapters.asClass(owner, anchor.getResolveScope());

        if (!TestInsanityUtil.psiNameIsSet(ownerClass))
        {
            return;
        }

        RenameTestService renameTestService = RenameTestService.getInstance(anchor.getProject());

        if (!renameTestService.getTestClassSiblingMediator().isTestClass(ownerClass))
        {
            List<PsiClass> testClasses = renameTestService.findTestClasses(ownerClass);

            if (testClasses.isEmpty())
            {
                return;
            }

            String message = "Class Tested (Found " + testClasses.size() + ")";

            result.add(createMarker(anchor, GUTTER_CLASS_ICON, testClasses, message, message));

            return;
        }

        PsiClass subjectClass = renameTestService.findSubjectClass(ownerClass);

        if (subjectClass == null)
        {
            result.add(
                createMarker(
                    anchor, GUTTER_CLASS_ORPHAN_ICON, emptyList(),
                    NO_SUBJECT_CLASS_MESSAGE, NO_SUBJECT_CLASS_MESSAGE));

            return;
        }

        String subjectClassPresentation = ClassPresentationUtil.getNameForClass(subjectClass, true);

        result.add(
            createMarker(
                anchor, GUTTER_CLASS_ICON, singletonList(subjectClass),
                "Subject Class " + subjectClassPresentation,
                "Subject Class <a href=\"#javaClass/" + subjectClassPresentation + "\">"
                    + getAbbreviatedText(subjectClassPresentation, MAX_FQN_LENGTH) + "</a>"));
    }

    protected void collectMethodMarker(
        PsiElement anchor, PsiElement owner, Collection<? super RelatedItemLineMarkerInfo<?>> result
    )
    {
        PsiMethod ownerMethod = TestElementAdapters.asMethod(owner);

        if (ownerMethod == null)
        {
            return;
        }

        PsiClass containingClass = ownerMethod.getContainingClass();

        if (!TestInsanityUtil.psiNameIsSet(containingClass))
        {
            return;
        }

        Project project = anchor.getProject();

        RenameTestService renameTestService = RenameTestService.getInstance(project);

        boolean annotationCheckEnabled = !TestInsanitySettings.getInstance(project).getTestAnnotations().isEmpty();

        PsiClass testClass = renameTestService.resolveTestClass(ownerMethod);

        if (testClass == null)
        {
            List<PsiClass> testClasses = renameTestService.findTestClasses(containingClass);

            addMethodMarker(
                anchor, testClasses,
                renameTestService.getTestMethodSiblingMediator().getTestMethods(ownerMethod, testClasses),
                "Test", true, result);

            return;
        }

        if (
            annotationCheckEnabled
                && !renameTestService.getTestMethodSiblingMediator().checkMethodAnnotation(ownerMethod, true)
        )
        {
            return;
        }

        PsiClass subjectClass = renameTestService.findSubjectClass(testClass);

        addMethodMarker(
            anchor,
            (subjectClass == null) ? emptyList() : singletonList(subjectClass),
            (subjectClass == null)
                ? emptyList()
                : renameTestService.getTestMethodSiblingMediator().getSubjectMethods(ownerMethod, subjectClass),
            "Subject", !annotationCheckEnabled, result);
    }

    protected void addMethodMarker(
        PsiElement anchor, List<PsiClass> siblingClasses, List<PsiMethod> siblingMethods,
        String siblingIdentifier, boolean ignoreMissing,
        Collection<? super RelatedItemLineMarkerInfo<?>> result
    )
    {
        if (siblingClasses.isEmpty() || siblingMethods.isEmpty())
        {
            if (ignoreMissing)
            {
                return;
            }

            String message = siblingClasses.isEmpty() ? NO_SUBJECT_CLASS_MESSAGE : NOT_LINKED_TO_SUBJECT_MESSAGE;

            result.add(createMarker(anchor, GUTTER_METHOD_ORPHAN_ICON, emptyList(), message, message));

            return;
        }

        PsiMethod siblingMethod = siblingMethods.get(0);

        String siblingMethodClassPresentation = ClassPresentationUtil.getContextName(siblingMethod, true);

        String siblingMethodPresentation =
            getAbbreviatedText(
                siblingMethodClassPresentation + "." + siblingMethod.getName(), MAX_FQN_LENGTH);

        String message =
            siblingIdentifier + "Method " + siblingMethodPresentation + " (" + siblingMethods.size() + " Found)";

        String tooltip =
            siblingIdentifier
                + "Method <a href=\"#javaClass/" + siblingMethodClassPresentation + "\">"
                + siblingMethodPresentation + "</a>"
                + " (" + siblingMethods.size() + " Found)";

        String displayName = getDisplayName(siblingMethod);

        if (displayName != null)
        {
            message += "<br/>Display Name " + displayName;
            tooltip +=
                "<br/>Display Name <a href=\"#javaClass/" + siblingMethodClassPresentation + "\">"
                    + displayName + "</a>";
        }

        result.add(createMarker(anchor, GUTTER_METHOD_ICON, siblingMethods, message, tooltip));
    }

    private static String getDisplayName(PsiMethod method)
    {
        PsiAnnotation displayNameAnnotation = method.getAnnotation(DISPLAY_NAME_FQN);

        if (displayNameAnnotation == null)
        {
            return null;
        }

        List<JvmAnnotationAttribute> attributes = displayNameAnnotation.getAttributes();

        if (attributes.isEmpty())
        {
            return null;
        }

        JvmAnnotationAttributeValue attributeValue = attributes.get(0).getAttributeValue();

        return (attributeValue instanceof JvmAnnotationConstantValue)
            ? String.valueOf(((JvmAnnotationConstantValue) attributeValue).getConstantValue())
            : null;
    }

    private static RelatedItemLineMarkerInfo<PsiElement> createMarker(
        PsiElement anchor, Icon icon, Collection<? extends PsiElement> targets, String title, String tooltip
    )
    {
        return NavigationGutterIconBuilder
            .create(icon)
            .setTargets(targets)
            .setPopupTitle(title)
            .setEmptyPopupText(title)
            .setTooltipText(tooltip)
            .createLineMarkerInfo(anchor);
    }

    private static String getAbbreviatedText(String text, int maxLength)
    {
        return StringUtils.abbreviate(text, text.length() - maxLength, maxLength + 4);
    }
}
