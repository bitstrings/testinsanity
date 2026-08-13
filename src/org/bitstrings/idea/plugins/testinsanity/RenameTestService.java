package org.bitstrings.idea.plugins.testinsanity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.bitstrings.idea.plugins.testinsanity.util.TestInsanityUtil;

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
import com.intellij.serviceContainer.NonInjectable;

public class RenameTestService
{
    private static final class Mediators
    {
        private final TestClassSiblingMediator classMediator;

        private final TestMethodSiblingMediator methodMediator;

        Mediators(TestClassSiblingMediator classMediator, TestMethodSiblingMediator methodMediator)
        {
            this.classMediator = classMediator;
            this.methodMediator = methodMediator;
        }
    }

    private volatile Mediators mediators;

    private final TestInsanitySettings settings;

    private final SimpleModificationTracker configurationTracker = new SimpleModificationTracker();

    public RenameTestService(Project project)
    {
        this(project, null, null);

        update();
    }

    @NonInjectable
    public RenameTestService(
        Project project,
        TestClassSiblingMediator testClassSiblingMediator,
        TestMethodSiblingMediator testMethodSiblingMediator
    )
    {
        this.mediators = new Mediators(testClassSiblingMediator, testMethodSiblingMediator);

        this.settings = TestInsanitySettings.getInstance(project);
    }

    public static RenameTestService getInstance(Project project)
    {
        return project.getService(RenameTestService.class);
    }

    public TestClassSiblingMediator getTestClassSiblingMediator()
    {
        return mediators.classMediator;
    }

    public TestMethodSiblingMediator getTestMethodSiblingMediator()
    {
        return mediators.methodMediator;
    }

    public List<PsiClass> findTestClasses(PsiClass subjectClass)
    {
        return CachedValuesManager.getCachedValue(
            subjectClass,
            () -> CachedValueProvider.Result.create(
                mediators.classMediator.getTestClasses(subjectClass, getSearchScope(subjectClass)),
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
                mediators.classMediator.getSubjectClass(testClass, getSearchScope(testClass)),
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

        PsiClass testClass = mediators.classMediator.resolveTestClass(containingClass);

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

        TestClassSiblingMediator classMediator = mediators.classMediator;

        for (PsiClass testClass : findTestClasses(subjectClass))
        {
            String newTestClassName =
                classMediator.renameTestName(testClass.getName(), subjectClass.getName(), newSubjectName);

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
            mediators.classMediator.renameSubjectName(subjectClass.getName(), testClass.getName(), newTestName);

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

        TestMethodSiblingMediator methodMediator = mediators.methodMediator;

        for (PsiMethod testMethod : methodMediator.getTestMethods(subjectMethod, findTestClasses(subjectClass)))
        {
            String newTestMethodName =
                methodMediator.renameTestName(testMethod.getName(), subjectMethod.getName(), newSubjectName);

            if (!Objects.equals(newTestMethodName, testMethod.getName()))
            {
                renames.put(testMethod, newTestMethodName);
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

        TestMethodSiblingMediator methodMediator = mediators.methodMediator;

        List<PsiMethod> subjectMethods = methodMediator.getSubjectMethods(testMethod, subjectClass);

        if (subjectMethods.isEmpty())
        {
            return renames;
        }

        String newSubjectName =
            methodMediator.renameSubjectName(subjectMethods.get(0).getName(), testMethod.getName(), newTestName);

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
        List<TestClassSiblingMediator> classMediators = new ArrayList<>();

        for (String testClassPattern : settings.resolveTestClassPatterns())
        {
            classMediators.add(
                new PatternBasedTestClassSiblingMediator(
                    testClassPattern, settings.isIncludeInterfacesAbstracts()));
        }

        List<TestMethodSiblingMediator> methodMediators = new ArrayList<>();

        for (String testMethodNamePattern : settings.resolveTestMethodNamePatterns())
        {
            methodMediators.add(
                new PatternBasedTestMethodSiblingMediator(
                    testMethodNamePattern,
                    settings.getTestMethodNameCapitalizationScheme(),
                    settings.getTestAnnotations(),
                    settings.isIncludeInheritedMethods(),
                    settings.isIncludeNestedClasses()));
        }

        mediators =
            new Mediators(
                new CompositeTestClassSiblingMediator(classMediators),
                new CompositeTestMethodSiblingMediator(methodMediators));

        configurationTracker.incModificationCount();
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

        TestClassSiblingMediator classMediator = mediators.classMediator;

        for (PsiClass candidate : candidates)
        {
            if (classMediator.isTestClass(candidate))
            {
                return candidate;
            }
        }

        return null;
    }
}
