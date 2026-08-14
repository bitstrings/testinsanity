package org.bitstrings.idea.plugins.testinsanity.intentions;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JList;

import org.bitstrings.idea.plugins.testinsanity.RenameTestService;
import org.bitstrings.idea.plugins.testinsanity.TestInsanityBundle;
import org.bitstrings.idea.plugins.testinsanity.TestScheme;
import org.bitstrings.idea.plugins.testinsanity.TestSchemes;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanityConfiguration;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings.TestAnnotation;
import org.bitstrings.idea.plugins.testinsanity.util.TestDisplayNames;
import org.bitstrings.idea.plugins.testinsanity.util.TestInsanityUtil;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;

public class CreateTestSiblingIntention
    extends PsiElementBaseIntentionAction
{
    private static final class CreateTarget
    {
        private final TestScheme scheme;

        private final PsiClass testClass;

        private final String testClassName;

        private final String testMethodName;

        CreateTarget(TestScheme scheme, PsiClass testClass, String testClassName, String testMethodName)
        {
            this.scheme = scheme;
            this.testClass = testClass;
            this.testClassName = testClassName;
            this.testMethodName = testMethodName;
        }

        String presentation()
        {
            if (testMethodName == null)
            {
                return TestInsanityBundle.message("testinsanity.intention.create.class", testClassName);
            }

            return (testClass == null)
                ? TestInsanityBundle
                    .message("testinsanity.intention.create.class.method", testMethodName, testClassName)
                : TestInsanityBundle
                    .message("testinsanity.intention.create.method", testMethodName, testClass.getName());
        }
    }

    private static final class TargetCellRenderer
        extends ColoredListCellRenderer<CreateTarget>
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected void customizeCellRenderer(
            JList<? extends CreateTarget> list, CreateTarget target, int index, boolean selected, boolean hasFocus
        )
        {
            append(target.presentation());
            append(" " + target.scheme.getName(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }

    private static final List<TestAnnotation> TEST_ANNOTATION_PREFERENCE =
        List.of(TestAnnotation.JUNIT5, TestAnnotation.JUNIT4, TestAnnotation.TESTNG);

    @Override
    public String getFamilyName()
    {
        return TestInsanityBundle.message("testinsanity.intention.family");
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiElement element)
    {
        List<CreateTarget> targets = findTargets(project, element);

        if (targets.isEmpty())
        {
            return false;
        }

        setText(
            (targets.size() == 1)
                ? targets.get(0).presentation()
                : TestInsanityBundle.message("testinsanity.intention.create.choose"));

        return true;
    }

    @Override
    public boolean startInWriteAction()
    {
        return false;
    }

    @Override
    public IntentionPreviewInfo generatePreview(Project project, Editor editor, PsiFile psiFile)
    {
        return IntentionPreviewInfo.EMPTY;
    }

    @Override
    public void invoke(Project project, Editor editor, PsiElement element)
    {
        PsiClass subjectClass = findSubjectClass(project, element);

        if (subjectClass == null)
        {
            return;
        }

        List<CreateTarget> targets = findTargets(project, element);

        if (targets.size() == 1)
        {
            createAndNavigate(project, subjectClass, targets.get(0));

            return;
        }

        if (editor == null)
        {
            for (CreateTarget target : targets)
            {
                createAndNavigate(project, subjectClass, target);
            }

            return;
        }

        JBPopupFactory
            .getInstance()
            .createPopupChooserBuilder(targets)
            .setTitle(TestInsanityBundle.message("testinsanity.intention.create.choose.title"))
            .setRenderer(new TargetCellRenderer())
            .setItemChosenCallback(target -> createAndNavigate(project, subjectClass, target))
            .createPopup()
            .showInBestPositionFor(editor);
    }

    private List<CreateTarget> findTargets(Project project, PsiElement element)
    {
        PsiClass subjectClass = findSubjectClass(project, element);

        if (subjectClass == null)
        {
            return List.of();
        }

        RenameTestService renameTestService = RenameTestService.getInstance(project);

        TestSchemes schemes = renameTestService.getTestSchemes();

        List<PsiClass> testClasses = renameTestService.findTestClasses(subjectClass);

        PsiMethod subjectMethod = findSubjectMethod(element);

        boolean canCreateClass = (SiblingSourceRoots.findTestSourceRoot(subjectClass) != null);

        List<CreateTarget> targets = new ArrayList<>();

        for (TestScheme scheme : schemes.getSchemes())
        {
            CreateTarget target =
                findTarget(project, schemes, scheme, subjectClass, subjectMethod, testClasses, canCreateClass);

            if (target != null)
            {
                targets.add(target);
            }
        }

        return targets;
    }

    private static CreateTarget findTarget(
        Project project, TestSchemes schemes, TestScheme scheme, PsiClass subjectClass, PsiMethod subjectMethod,
        List<PsiClass> testClasses, boolean canCreateClass
    )
    {
        PsiClass testClass = schemes.testClassOf(scheme, testClasses);

        String testClassName = schemes.generateTestClassName(scheme, subjectClass.getName());

        if ((testClass == null) && (!canCreateClass || !isIdentifier(project, testClassName)))
        {
            return null;
        }

        if (subjectMethod == null)
        {
            return (testClass == null)
                ? new CreateTarget(scheme, null, testClassName, null)
                : null;
        }

        String testMethodName = schemes.generateTestMethodName(scheme, subjectMethod.getName());

        if (!isIdentifier(project, testMethodName))
        {
            return null;
        }

        if (testClass == null)
        {
            return new CreateTarget(scheme, null, testClassName, testMethodName);
        }

        return (scheme.getMethodMediator().getTestMethods(subjectMethod, List.of(testClass)).isEmpty()
            && isModifiableJavaClass(testClass))
                ? new CreateTarget(scheme, testClass, testClassName, testMethodName)
                : null;
    }

    private void createAndNavigate(Project project, PsiClass subjectClass, CreateTarget target)
    {
        PsiElement created =
            WriteCommandAction
                .writeCommandAction(project)
                .withName(target.presentation())
                .compute(() -> createSibling(project, subjectClass, target));

        if (created instanceof Navigatable)
        {
            ((Navigatable) created).navigate(true);
        }
    }

    private static PsiElement createSibling(Project project, PsiClass subjectClass, CreateTarget target)
    {
        PsiClass testClass =
            (target.testClass == null)
                ? createTestClass(project, subjectClass, target.testClassName)
                : target.testClass;

        if (testClass == null)
        {
            return null;
        }

        return (target.testMethodName == null)
            ? testClass
            : createTestMethod(project, testClass, target.testMethodName);
    }

    private static PsiClass findSubjectClass(Project project, PsiElement element)
    {
        if (!(element.getContainingFile() instanceof PsiJavaFile))
        {
            return null;
        }

        PsiClass elementClass = PsiTreeUtil.getParentOfType(element, PsiClass.class, false);

        if (!TestInsanityUtil.psiNameIsSet(elementClass) || isInTestSources(project, elementClass))
        {
            return null;
        }

        return (RenameTestService.getInstance(project).getTestSchemes().resolveTestClass(elementClass) == null)
            ? elementClass
            : null;
    }

    private static PsiMethod findSubjectMethod(PsiElement element)
    {
        PsiMethod elementMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);

        return ((elementMethod == null) || elementMethod.isConstructor() || (elementMethod.getName() == null))
            ? null
            : elementMethod;
    }

    private static PsiClass createTestClass(Project project, PsiClass subjectClass, String testClassName)
    {
        VirtualFile testSourceRoot = SiblingSourceRoots.findTestSourceRoot(subjectClass);

        if (testSourceRoot == null)
        {
            return null;
        }

        PsiDirectory testDirectory =
            SiblingSourceRoots
                .findOrCreateDirectory(project, testSourceRoot, PsiUtil.getPackageName(subjectClass));

        return (testDirectory == null)
            ? null
            : JavaDirectoryService.getInstance().createClass(testDirectory, testClassName);
    }

    private static PsiMethod createTestMethod(Project project, PsiClass testClass, String testMethodName)
    {
        TestInsanityConfiguration configuration = TestInsanityConfiguration.getInstance(project);

        StringBuilder methodText = new StringBuilder();

        String testAnnotationFqn = resolveTestAnnotationFqn(project, testClass, configuration);

        if (testAnnotationFqn != null)
        {
            methodText.append('@').append(testAnnotationFqn).append('\n');
        }

        if (configuration.isSyncDisplayName()
            && (findClass(project, testClass, TestDisplayNames.DISPLAY_NAME_FQN) != null))
        {
            methodText
                .append('@').append(TestDisplayNames.DISPLAY_NAME_FQN)
                .append("(\"")
                .append(StringUtil.escapeStringCharacters(TestDisplayNames.humanize(testMethodName)))
                .append("\")\n");
        }

        methodText.append("public void ").append(testMethodName).append("()\n{\n}");

        PsiMethod testMethod =
            (PsiMethod) testClass
                .add(JavaPsiFacade.getElementFactory(project).createMethodFromText(methodText.toString(), testClass));

        JavaCodeStyleManager.getInstance(project).shortenClassReferences(testMethod);

        return testMethod;
    }

    private static String resolveTestAnnotationFqn(
        Project project, PsiClass testClass, TestInsanityConfiguration configuration
    )
    {
        for (TestAnnotation testAnnotation : TEST_ANNOTATION_PREFERENCE)
        {
            if (
                configuration.isTestAnnotationEnabled(testAnnotation)
                    && (findClass(project, testClass, testAnnotation.getPrimaryAnnotationFqn()) != null)
            )
            {
                return testAnnotation.getPrimaryAnnotationFqn();
            }
        }

        return null;
    }

    private static boolean isInTestSources(Project project, PsiClass elementClass)
    {
        VirtualFile classFile = elementClass.getContainingFile().getVirtualFile();

        return (classFile != null) && ProjectFileIndex.getInstance(project).isInTestSourceContent(classFile);
    }

    private static boolean isModifiableJavaClass(PsiClass testClass)
    {
        return (testClass.getContainingFile() instanceof PsiJavaFile) && testClass.isWritable();
    }

    private static boolean isIdentifier(Project project, String name)
    {
        return PsiNameHelper.getInstance(project).isIdentifier(name);
    }

    private static PsiClass findClass(Project project, PsiClass contextClass, String classFqn)
    {
        return JavaPsiFacade.getInstance(project).findClass(classFqn, contextClass.getResolveScope());
    }
}
