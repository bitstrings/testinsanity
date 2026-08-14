package org.bitstrings.idea.plugins.testinsanity.intentions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.TestModuleProperties;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;

public final class SiblingSourceRoots
{
    private SiblingSourceRoots()
    {
    }

    public static VirtualFile findTestSourceRoot(PsiClass subjectClass)
    {
        Module module = ModuleUtilCore.findModuleForPsiElement(subjectClass);

        if (module == null)
        {
            return null;
        }

        VirtualFile ownRoot = firstSourceRoot(module, JavaSourceRootType.TEST_SOURCE);

        if (ownRoot != null)
        {
            return ownRoot;
        }

        Module testModule = findDeclaredTestModule(module);

        return (testModule == null)
            ? firstSourceRoot(findSoleDependentWithTests(module), JavaSourceRootType.TEST_SOURCE)
            : firstSourceRoot(testModule, JavaSourceRootType.TEST_SOURCE);
    }

    public static VirtualFile findProductionSourceRoot(PsiClass testClass)
    {
        Module module = ModuleUtilCore.findModuleForPsiElement(testClass);

        if (module == null)
        {
            return null;
        }

        VirtualFile ownRoot = firstSourceRoot(module, JavaSourceRootType.SOURCE);

        return (ownRoot == null)
            ? firstSourceRoot(TestModuleProperties.getInstance(module).getProductionModule(), JavaSourceRootType.SOURCE)
            : ownRoot;
    }

    public static PsiDirectory findOrCreateDirectory(Project project, VirtualFile sourceRoot, String packageName)
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

    private static Module findDeclaredTestModule(Module productionModule)
    {
        for (Module candidate : ModuleManager.getInstance(productionModule.getProject()).getModules())
        {
            if (productionModule.equals(TestModuleProperties.getInstance(candidate).getProductionModule()))
            {
                return candidate;
            }
        }

        return null;
    }

    private static Module findSoleDependentWithTests(Module module)
    {
        Set<Module> dependents = new HashSet<>();

        ModuleUtilCore.collectModulesDependsOn(module, dependents);

        dependents.remove(module);

        Module withTests = null;

        for (Module dependent : dependents)
        {
            if (firstSourceRoot(dependent, JavaSourceRootType.TEST_SOURCE) != null)
            {
                if (withTests != null)
                {
                    return null;
                }

                withTests = dependent;
            }
        }

        return withTests;
    }

    private static VirtualFile firstSourceRoot(Module module, JavaSourceRootType rootType)
    {
        if (module == null)
        {
            return null;
        }

        List<VirtualFile> sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots(rootType);

        return sourceRoots.isEmpty() ? null : sourceRoots.get(0);
    }
}
