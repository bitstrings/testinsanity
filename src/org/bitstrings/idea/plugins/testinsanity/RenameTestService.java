package org.bitstrings.idea.plugins.testinsanity;

import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanityConfiguration;
import org.bitstrings.idea.plugins.testinsanity.util.TestInsanityUtil;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;

public final class RenameTestService
{
    private volatile TestSchemes schemes;

    private final Project project;

    private final TestSchemesFactory schemesFactory;

    private final SimpleModificationTracker configurationTracker = new SimpleModificationTracker();

    public RenameTestService(Project project)
    {
        this.project = project;
        this.schemesFactory = new TestSchemesFactory(TestInsanityConfiguration.getInstance(project));

        reload();
    }

    public static RenameTestService getInstance(Project project)
    {
        return project.getService(RenameTestService.class);
    }

    public TestSchemes getTestSchemes()
    {
        return schemes;
    }

    public List<PsiClass> findTestClasses(PsiClass subjectClass)
    {
        return CachedValuesManager.getCachedValue(
            subjectClass,
            () -> CachedValueProvider.Result.create(
                schemes.getTestClasses(subjectClass, getSearchScope(subjectClass)),
                PsiModificationTracker.MODIFICATION_COUNT,
                ProjectRootManager.getInstance(subjectClass.getProject()),
                configurationTracker
            )
        );
    }

    public PsiClass findSubjectClass(PsiClass testClass)
    {
        return CachedValuesManager.getCachedValue(
            testClass,
            () -> CachedValueProvider.Result.create(
                schemes.getSubjectClass(testClass, getSearchScope(testClass)),
                PsiModificationTracker.MODIFICATION_COUNT,
                ProjectRootManager.getInstance(testClass.getProject()),
                configurationTracker
            )
        );
    }

    public PsiClass resolveTestClass(PsiMethod method)
    {
        PsiClass containingClass = method.getContainingClass();

        if (!TestInsanityUtil.psiNameIsSet(containingClass))
        {
            return null;
        }

        PsiClass testClass = schemes.resolveTestClass(containingClass);

        return (testClass == null)
            ? findInheritedTestClass(containingClass)
            : testClass;
    }

    public PsiClass findInheritedTestClass(PsiClass baseClass)
    {
        if (!isInTestSources(baseClass))
        {
            return null;
        }

        return CachedValuesManager.getCachedValue(
            baseClass,
            () -> CachedValueProvider.Result.create(
                computeInheritedTestClass(baseClass),
                PsiModificationTracker.MODIFICATION_COUNT,
                ProjectRootManager.getInstance(baseClass.getProject()),
                configurationTracker
            )
        );
    }

    public Map<PsiClass, String> renameSubjectClassMapping(PsiClass subjectClass, String newSubjectName)
    {
        Map<PsiClass, String> renames = new LinkedHashMap<>();

        if (!TestInsanityUtil.psiNameIsSet(subjectClass))
        {
            return renames;
        }

        TestSchemes testSchemes = schemes;

        for (PsiClass testClass : findTestClasses(subjectClass))
        {
            String newTestClassName =
                testSchemes.renameTestClassName(testClass.getName(), subjectClass.getName(), newSubjectName);

            if (!Objects.equals(newTestClassName, testClass.getName()))
            {
                renames.put(testClass, newTestClassName);
            }
        }

        return renames;
    }

    public Map<PsiClass, String> renameTestClassMapping(PsiClass testClass, String newTestName)
    {
        Map<PsiClass, String> renames = new LinkedHashMap<>();

        if (!TestInsanityUtil.psiNameIsSet(testClass))
        {
            return renames;
        }

        PsiClass subjectClass = findSubjectClass(testClass);

        if (!TestInsanityUtil.psiNameIsSet(subjectClass))
        {
            return renames;
        }

        String newSubjectName =
            schemes.renameSubjectClassNameXXX(subjectClass.getName(), testClass.getName(), newTestName);

        if (StringUtils.isEmpty(newSubjectName))
        {
            return renames;
        }

        if (!Objects.equals(subjectClass.getName(), newSubjectName))
        {
            renames.put(subjectClass, newSubjectName);
        }

        renames.putAll(renameSubjectClassMapping(subjectClass, newSubjectName));

        renames.remove(testClass);

        return renames;
    }

    public Map<PsiMethod, String> renameSubjectMethodMapping(PsiMethod subjectMethod, String newSubjectName)
    {
        Map<PsiMethod, String> renames = new LinkedHashMap<>();

        PsiClass subjectClass = subjectMethod.getContainingClass();

        if (!TestInsanityUtil.psiNameIsSet(subjectClass))
        {
            return renames;
        }

        TestSchemes testSchemes = schemes;

        for (PsiClass testClass : findTestClasses(subjectClass))
        {
            for (PsiMethod testMethod : testSchemes.getTestMethods(subjectMethod, singletonList(testClass)))
            {
                String newTestMethodName =
                    testSchemes.renameTestMethodName(
                        testClass, testMethod.getName(), subjectMethod.getName(), newSubjectName);

                if (!Objects.equals(newTestMethodName, testMethod.getName()))
                {
                    renames.put(testMethod, newTestMethodName);
                }
            }
        }

        return renames;
    }

    public Map<PsiMethod, String> renameTestMethodMapping(PsiMethod testMethod, String newTestName)
    {
        Map<PsiMethod, String> renames = new LinkedHashMap<>();

        PsiClass testClass = resolveTestClass(testMethod);

        if (testClass == null)
        {
            return renames;
        }

        PsiClass subjectClass = findSubjectClass(testClass);

        if (subjectClass == null)
        {
            return renames;
        }

        TestSchemes testSchemes = schemes;

        List<PsiMethod> subjectMethods = testSchemes.getSubjectMethods(testClass, testMethod, subjectClass);

        if (subjectMethods.isEmpty())
        {
            return renames;
        }

        String newSubjectName =
            testSchemes.renameSubjectMethodName(
                testClass, subjectMethods.get(0).getName(), testMethod.getName(), newTestName);

        if (StringUtils.isEmpty(newSubjectName))
        {
            return renames;
        }

        for (PsiMethod subjectMethod : subjectMethods)
        {
            if (!Objects.equals(subjectMethod.getName(), newSubjectName))
            {
                renames.put(subjectMethod, newSubjectName);
            }

            renames.putAll(renameSubjectMethodMapping(subjectMethod, newSubjectName));
        }

        renames.remove(testMethod);

        return renames;
    }

    public void update()
    {
        reload();

        restartHighlighting();
    }

    private void reload()
    {
        schemes = schemesFactory.createLenient();

        configurationTracker.incModificationCount();
    }

    private void restartHighlighting()
    {
        DaemonCodeAnalyzer.getInstance(project).settingsChanged();
    }

    public GlobalSearchScope getSearchScope(PsiElement element)
    {
        Module module = ModuleUtilCore.findModuleForPsiElement(element);

        return (module == null)
            ? GlobalSearchScope.EMPTY_SCOPE
            : GlobalSearchScope
                .moduleWithDependentsScope(module)
                .union(GlobalSearchScope.moduleWithDependenciesScope(module));
    }

    private static boolean isInTestSources(PsiClass psiClass)
    {
        PsiFile containingFile = psiClass.getContainingFile();

        VirtualFile classFile = (containingFile == null) ? null : containingFile.getVirtualFile();

        return (classFile != null)
            && ProjectFileIndex.getInstance(psiClass.getProject()).isInTestSourceContent(classFile);
    }

    private PsiClass computeInheritedTestClass(PsiClass baseClass)
    {
        Project project = baseClass.getProject();

        List<PsiClass> candidates =
            new ArrayList<>(
                ClassInheritorsSearch
                    .search(
                        baseClass,
                        getSearchScope(baseClass).intersectWith(GlobalSearchScopes.projectTestScope(project)),
                        true
                    )
                    .findAll());

        candidates.sort(TestInsanityUtil.STABLE_CLASS_ORDER);

        TestSchemes testSchemes = schemes;

        for (PsiClass candidate : candidates)
        {
            if (testSchemes.isTestClass(candidate))
            {
                return candidate;
            }
        }

        return null;
    }
}
