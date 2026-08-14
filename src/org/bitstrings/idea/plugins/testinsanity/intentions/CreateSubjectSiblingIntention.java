package org.bitstrings.idea.plugins.testinsanity.intentions;

import org.bitstrings.idea.plugins.testinsanity.RenameTestService;
import org.bitstrings.idea.plugins.testinsanity.TestInsanityBundle;
import org.bitstrings.idea.plugins.testinsanity.TestSchemes;
import org.bitstrings.idea.plugins.testinsanity.util.TestInsanityUtil;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
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
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;

public class CreateSubjectSiblingIntention
    extends PsiElementBaseIntentionAction
{
    @Override
    public String getFamilyName()
    {
        return TestInsanityBundle.message("testinsanity.intention.subject.family");
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiElement element)
    {
        PsiClass testClass = findTestClass(project, element);

        if (testClass == null)
        {
            return false;
        }

        RenameTestService renameTestService = RenameTestService.getInstance(project);

        PsiClass subjectClass = renameTestService.findSubjectClass(testClass);

        if (subjectClass == null)
        {
            String subjectClassName = renameTestService.getTestSchemes().findSubjectClassName(testClass);

            if (!canCreateSubjectClass(project, testClass, subjectClassName))
            {
                return false;
            }

            setText(TestInsanityBundle.message("testinsanity.intention.subject.create.class", subjectClassName));

            return true;
        }

        String subjectMethodName = findMissingSubjectMethodName(project, testClass, subjectClass, element);

        if (subjectMethodName == null)
        {
            return false;
        }

        setText(
            TestInsanityBundle
                .message(
                    "testinsanity.intention.subject.create.method", subjectMethodName, subjectClass.getName()));

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
        PsiClass testClass = findTestClass(project, element);

        if (testClass == null)
        {
            return;
        }

        PsiElement created =
            WriteCommandAction
                .writeCommandAction(project)
                .withName(getText())
                .compute(() -> createSibling(project, testClass, element));

        if (created instanceof Navigatable)
        {
            ((Navigatable) created).navigate(true);
        }
    }

    private static PsiElement createSibling(Project project, PsiClass testClass, PsiElement element)
    {
        RenameTestService renameTestService = RenameTestService.getInstance(project);

        PsiClass subjectClass = renameTestService.findSubjectClass(testClass);

        if (subjectClass == null)
        {
            return createSubjectClass(
                project, testClass, renameTestService.getTestSchemes().findSubjectClassName(testClass));
        }

        String subjectMethodName = findMissingSubjectMethodName(project, testClass, subjectClass, element);

        return (subjectMethodName == null)
            ? null
            : createSubjectMethod(project, subjectClass, subjectMethodName);
    }

    private static PsiClass findTestClass(Project project, PsiElement element)
    {
        if (!(element.getContainingFile() instanceof PsiJavaFile))
        {
            return null;
        }

        PsiClass elementClass = PsiTreeUtil.getParentOfType(element, PsiClass.class, false);

        if (!TestInsanityUtil.psiNameIsSet(elementClass) || !isInTestSources(project, elementClass))
        {
            return null;
        }

        return RenameTestService.getInstance(project).getTestSchemes().resolveTestClass(elementClass);
    }

    private static String findMissingSubjectMethodName(
        Project project, PsiClass testClass, PsiClass subjectClass, PsiElement element
    )
    {
        PsiMethod testMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);

        if ((testMethod == null) || testMethod.isConstructor() || (testMethod.getName() == null))
        {
            return null;
        }

        TestSchemes schemes = RenameTestService.getInstance(project).getTestSchemes();

        if (!schemes.getSubjectMethods(testClass, testMethod, subjectClass).isEmpty())
        {
            return null;
        }

        String subjectMethodName = schemes.findSubjectMethodName(testClass, testMethod.getName());

        if ((subjectMethodName == null)
            || !isIdentifier(project, subjectMethodName)
            || !isModifiableJavaClass(subjectClass))
        {
            return null;
        }

        return (subjectClass.findMethodsByName(subjectMethodName, true).length == 0) ? subjectMethodName : null;
    }

    private static boolean canCreateSubjectClass(Project project, PsiClass testClass, String subjectClassName)
    {
        return (subjectClassName != null)
            && isIdentifier(project, subjectClassName)
            && (SiblingSourceRoots.findProductionSourceRoot(testClass) != null);
    }

    private static PsiClass createSubjectClass(Project project, PsiClass testClass, String subjectClassName)
    {
        VirtualFile productionSourceRoot = SiblingSourceRoots.findProductionSourceRoot(testClass);

        if ((productionSourceRoot == null) || (subjectClassName == null))
        {
            return null;
        }

        PsiDirectory subjectDirectory =
            SiblingSourceRoots
                .findOrCreateDirectory(project, productionSourceRoot, PsiUtil.getPackageName(testClass));

        return (subjectDirectory == null)
            ? null
            : JavaDirectoryService.getInstance().createClass(subjectDirectory, subjectClassName);
    }

    private static PsiMethod createSubjectMethod(Project project, PsiClass subjectClass, String subjectMethodName)
    {
        return (PsiMethod) subjectClass
            .add(
                JavaPsiFacade
                    .getElementFactory(project)
                    .createMethodFromText("public void " + subjectMethodName + "()\n{\n}", subjectClass));
    }

    private static boolean isInTestSources(Project project, PsiClass elementClass)
    {
        VirtualFile classFile = elementClass.getContainingFile().getVirtualFile();

        return (classFile != null) && ProjectFileIndex.getInstance(project).isInTestSourceContent(classFile);
    }

    private static boolean isModifiableJavaClass(PsiClass psiClass)
    {
        return (psiClass.getContainingFile() instanceof PsiJavaFile) && psiClass.isWritable();
    }

    private static boolean isIdentifier(Project project, String name)
    {
        return (name != null) && PsiNameHelper.getInstance(project).isIdentifier(name);
    }
}
