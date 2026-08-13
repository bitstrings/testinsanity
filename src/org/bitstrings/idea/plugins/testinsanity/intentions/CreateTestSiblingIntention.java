package org.bitstrings.idea.plugins.testinsanity.intentions;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.testinsanity.RenameTestService;
import org.bitstrings.idea.plugins.testinsanity.TestInsanityBundle;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings.TestAnnotation;
import org.bitstrings.idea.plugins.testinsanity.util.TestDisplayNames;
import org.bitstrings.idea.plugins.testinsanity.util.TestInsanityUtil;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
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
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;

public class CreateTestSiblingIntention
    extends PsiElementBaseIntentionAction
{
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
        PsiClass subjectClass = findSubjectClass(project, element);

        if (subjectClass == null)
        {
            return false;
        }

        RenameTestService renameTestService = RenameTestService.getInstance(project);

        List<PsiClass> testClasses = renameTestService.findTestClasses(subjectClass);

        PsiMethod subjectMethod = findSubjectMethod(element);

        String testClassName =
            renameTestService.getTestClassSiblingMediator().generateTestName(subjectClass.getName());

        if (subjectMethod == null)
        {
            if (!testClasses.isEmpty() || !canCreateTestClass(project, subjectClass, testClassName))
            {
                return false;
            }

            setText(TestInsanityBundle.message("testinsanity.intention.create.class", testClassName));

            return true;
        }

        String testMethodName =
            renameTestService.getTestMethodSiblingMediator().generateTestName(subjectMethod.getName());

        if (!isIdentifier(project, testMethodName))
        {
            return false;
        }

        if (testClasses.isEmpty())
        {
            if (!canCreateTestClass(project, subjectClass, testClassName))
            {
                return false;
            }

            setText(
                TestInsanityBundle
                    .message("testinsanity.intention.create.class.method", testMethodName, testClassName));

            return true;
        }

        PsiClass testClass = testClasses.get(0);

        if (
            !renameTestService.getTestMethodSiblingMediator().getTestMethods(subjectMethod, testClasses).isEmpty()
                || !isModifiableJavaClass(testClass)
        )
        {
            return false;
        }

        setText(
            TestInsanityBundle
                .message("testinsanity.intention.create.method", testMethodName, testClass.getName()));

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

        PsiMethod subjectMethod = findSubjectMethod(element);

        PsiElement created =
            WriteCommandAction
                .writeCommandAction(project)
                .withName(getText())
                .compute(() -> createSibling(project, subjectClass, subjectMethod));

        if (created instanceof Navigatable)
        {
            ((Navigatable) created).navigate(true);
        }
    }

    private static PsiElement createSibling(Project project, PsiClass subjectClass, PsiMethod subjectMethod)
    {
        List<PsiClass> testClasses = RenameTestService.getInstance(project).findTestClasses(subjectClass);

        PsiClass testClass =
            testClasses.isEmpty()
                ? createTestClass(project, subjectClass)
                : testClasses.get(0);

        if (testClass == null)
        {
            return null;
        }

        return (subjectMethod == null)
            ? testClass
            : createTestMethod(project, testClass, subjectMethod);
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

        return (RenameTestService
            .getInstance(project)
            .getTestClassSiblingMediator()
            .resolveTestClass(elementClass) == null)
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

    private static boolean canCreateTestClass(Project project, PsiClass subjectClass, String testClassName)
    {
        return isIdentifier(project, testClassName) && (findTestSourceRoot(subjectClass) != null);
    }

    private static PsiClass createTestClass(Project project, PsiClass subjectClass)
    {
        VirtualFile testSourceRoot = findTestSourceRoot(subjectClass);

        if (testSourceRoot == null)
        {
            return null;
        }

        PsiDirectory testDirectory =
            findOrCreateDirectory(project, testSourceRoot, PsiUtil.getPackageName(subjectClass));

        if (testDirectory == null)
        {
            return null;
        }

        return JavaDirectoryService
            .getInstance()
            .createClass(
                testDirectory,
                RenameTestService
                    .getInstance(project)
                    .getTestClassSiblingMediator()
                    .generateTestName(subjectClass.getName()));
    }

    private static PsiMethod createTestMethod(Project project, PsiClass testClass, PsiMethod subjectMethod)
    {
        TestInsanitySettings settings = TestInsanitySettings.getInstance(project);

        String testMethodName =
            RenameTestService
                .getInstance(project)
                .getTestMethodSiblingMediator()
                .generateTestName(subjectMethod.getName());

        StringBuilder methodText = new StringBuilder();

        String testAnnotationFqn = resolveTestAnnotationFqn(project, testClass, settings);

        if (testAnnotationFqn != null)
        {
            methodText.append('@').append(testAnnotationFqn).append('\n');
        }

        if (settings.isSyncDisplayName() && (findClass(project, testClass, TestDisplayNames.DISPLAY_NAME_FQN) != null))
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
        Project project, PsiClass testClass, TestInsanitySettings settings
    )
    {
        for (TestAnnotation testAnnotation : TEST_ANNOTATION_PREFERENCE)
        {
            if (
                settings.hasTestAnnotation(testAnnotation)
                    && (findClass(project, testClass, testAnnotation.getPrimaryAnnotationFqn()) != null)
            )
            {
                return testAnnotation.getPrimaryAnnotationFqn();
            }
        }

        return null;
    }

    private static PsiDirectory findOrCreateDirectory(Project project, VirtualFile sourceRoot, String packageName)
    {
        PsiDirectory directory = PsiManager.getInstance(project).findDirectory(sourceRoot);

        if ((directory == null) || StringUtils.isEmpty(packageName))
        {
            return directory;
        }

        for (String packageSegment : StringUtils.split(packageName, '.'))
        {
            PsiDirectory subdirectory = directory.findSubdirectory(packageSegment);

            directory = (subdirectory == null) ? directory.createSubdirectory(packageSegment) : subdirectory;
        }

        return directory;
    }

    private static VirtualFile findTestSourceRoot(PsiClass subjectClass)
    {
        Module module = ModuleUtilCore.findModuleForPsiElement(subjectClass);

        if (module == null)
        {
            return null;
        }

        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(subjectClass.getProject());

        for (VirtualFile sourceRoot : ModuleRootManager.getInstance(module).getSourceRoots(true))
        {
            if (fileIndex.isInTestSourceContent(sourceRoot))
            {
                return sourceRoot;
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
