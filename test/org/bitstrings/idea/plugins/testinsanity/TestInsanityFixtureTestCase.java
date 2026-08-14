package org.bitstrings.idea.plugins.testinsanity;

import java.util.List;

import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.bitstrings.idea.plugins.testinsanity.config.TestSchemeSpec;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public abstract class TestInsanityFixtureTestCase
    extends BasePlatformTestCase
{
    protected static final String TEST_SOURCE_ROOT_NAME = "testSrc";

    private static final LightProjectDescriptor DESCRIPTOR =
        new LightProjectDescriptor()
        {
            @Override
            public String getModuleTypeId()
            {
                return "JAVA_MODULE";
            }

            @Override
            protected void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry)
            {
                super.configureModule(module, model, contentEntry);

                VirtualFile testSourceRoot =
                    doCreateSourceRoot(contentEntry.getFile().getParent(), TEST_SOURCE_ROOT_NAME);

                model.addContentEntry(testSourceRoot).addSourceFolder(testSourceRoot, JavaSourceRootType.TEST_SOURCE);
            }
        };

    @Override
    protected LightProjectDescriptor getProjectDescriptor()
    {
        return DESCRIPTOR;
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        clearSourceRoots();

        useSchemes();

        addTestSourceFile("Test.java", "package org.junit;\npublic @interface Test {}\n");
    }

    private void clearSourceRoots()
        throws Exception
    {
        WriteCommandAction
            .writeCommandAction(getProject())
            .run(
                () ->
                {
                    for (VirtualFile sourceRoot : ModuleRootManager
                        .getInstance(myFixture.getModule())
                        .getSourceRoots(true))
                    {
                        for (VirtualFile child : sourceRoot.getChildren())
                        {
                            child.delete(this);
                        }
                    }
                });
    }

    protected void useSchemes(TestSchemeSpec... schemes)
    {
        TestInsanitySettings.getInstance(getProject()).updateSchemes(List.of(schemes));

        RenameTestService.getInstance(getProject()).update();
    }

    protected static TestSchemeSpec scheme(String name, String testClassPattern, String testMethodPattern)
    {
        return new TestSchemeSpec(name, testClassPattern, List.of(testMethodPattern));
    }

    protected PsiClass addSubjectClass(String className, String body)
    {
        return ((PsiJavaFile) myFixture
            .addFileToProject(className + ".java", "public class " + className + "\n{\n" + body + "\n}\n"))
                .getClasses()[0];
    }

    protected PsiClass addTestClass(String className, String body)
        throws Exception
    {
        return ((PsiJavaFile) addTestSourceFile(
            className + ".java", "public class " + className + "\n{\n" + body + "\n}\n")).getClasses()[0];
    }

    protected PsiFile addTestSourceFile(String fileName, String text)
        throws Exception
    {
        return WriteCommandAction
            .writeCommandAction(getProject())
            .compute(
                () ->
                {
                    VirtualFile testSourceRoot = findTestSourceRoot();

                    VirtualFile existingFile = testSourceRoot.findChild(fileName);

                    VirtualFile file =
                        (existingFile == null) ? testSourceRoot.createChildData(this, fileName) : existingFile;

                    VfsUtil.saveText(file, text);

                    return PsiManager.getInstance(getProject()).findFile(file);
                });
    }

    protected VirtualFile findTestSourceRoot()
    {
        for (VirtualFile sourceRoot : ModuleRootManager.getInstance(myFixture.getModule()).getSourceRoots(true))
        {
            if (TEST_SOURCE_ROOT_NAME.equals(sourceRoot.getName()))
            {
                return sourceRoot;
            }
        }

        return null;
    }
}
