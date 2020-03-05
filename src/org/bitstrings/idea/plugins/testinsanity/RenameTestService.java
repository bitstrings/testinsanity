package org.bitstrings.idea.plugins.testinsanity;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher.CapitalizationScheme;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.serviceContainer.NonInjectable;

public class RenameTestService
{
    private TestClassSiblingMediator testClassSiblingMediator;

    private TestMethodSiblingMediator testMethodSiblingMediator;

    private final Project project;

    private final TestInsanitySettings settings;

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
        this.project = project;

        this.testClassSiblingMediator = testClassSiblingMediator;
        this.testMethodSiblingMediator = testMethodSiblingMediator;

        this.settings = TestInsanitySettings.getInstance(project);
    }

    public static RenameTestService getInstance(Project project)
    {
        return ServiceManager.getService(project, RenameTestService.class);
    }

    public TestClassSiblingMediator getTestClassSiblingMediator()
    {
        return testClassSiblingMediator;
    }

    public TestMethodSiblingMediator getTestMethodSiblingMediator()
    {
        return testMethodSiblingMediator;
    }

    public Map<PsiClass, String> renameSubjectClassMapping(
        PsiClass subjectClass, String newSubjectName, GlobalSearchScope searchScope
    )
    {
        String oldSubjectName = subjectClass.getName();

        Map<PsiClass, String> renames = new LinkedHashMap<>();

        List<PsiClass> testClasses = testClassSiblingMediator.getTestClasses(subjectClass, searchScope);

        for (PsiClass testClass : testClasses)
        {
            String newTestClassName =
                testClassSiblingMediator.renameTestName(testClass.getName(), oldSubjectName, newSubjectName);

            if (!Objects.equals(newTestClassName, oldSubjectName))
            {
                renames.put(testClass, newTestClassName);
            }
        }

        return renames;
    }

    public Map<PsiClass, String> renameTestClassMapping(
        PsiClass testClass, String newTestName, GlobalSearchScope searchScope
    )
    {
        String oldTestName = testClass.getName();

        Map<PsiClass, String> renames = new LinkedHashMap<>();

        PsiClass subjectClass = testClassSiblingMediator.getSubjectClass(testClass, searchScope);

        if (subjectClass != null)
        {
            String newSubjectName =
                testClassSiblingMediator
                    .renameSubjectName(subjectClass.getName(), oldTestName, newTestName);

            if (newSubjectName != null && !Objects.equals(subjectClass, newSubjectName))
            {
                renames.put(subjectClass, newSubjectName);
            }
        }

        return renames;
    }

    public Map<PsiMethod, String> renameSubjectMethodMapping(
        PsiMethod subjectMethod, String newSubjectName, GlobalSearchScope searchScope
    )
    {
        String oldSubjectName = subjectMethod.getName();

        PsiClass subjectClass = subjectMethod.getContainingClass();

        Map<PsiMethod, String> renames = new LinkedHashMap<>();

        List<PsiMethod> testMethods =
            testMethodSiblingMediator
                .getTestMethods(
                    subjectMethod,
                    testClassSiblingMediator
                        .getTestClasses(
                            subjectClass,
                            searchScope
                        )
                );

//        if (!Objects.equals(oldSubjectName, newSubjectName))
//        {
//            renames.put(subjectMethod, newSubjectName);
//        }

        for (PsiMethod testMethod : testMethods)
        {
            String newTestMethodName =
                testMethodSiblingMediator.renameTestName(testMethod.getName(), oldSubjectName, newSubjectName);

            if (!Objects.equals(newTestMethodName, testMethod.getName()))
            {
                renames.put(testMethod, newTestMethodName);
            }
        }

        return renames;
    }

    public Map<PsiMethod, String> renameTestMethodMapping(
        PsiMethod testMethod, String newTestName, GlobalSearchScope searchScope
    )
    {
        String oldTestName = testMethod.getName();

        PsiClass testClass = testMethod.getContainingClass();

        Map<PsiMethod, String> renames = new LinkedHashMap<>();

        if (!testClassSiblingMediator.isTestClassName(testClass.getName()))
        {
            Collection<PsiClass> testSubClasseCandidates =
                ClassInheritorsSearch
                    .search(
                        testClass,
                        searchScope.intersectWith(GlobalSearchScopes.projectTestScope(testMethod.getProject())),
                        true
                    )
                    .allowParallelProcessing()
                    .findAll();

            for (PsiClass testSubClassCandidate : testSubClasseCandidates)
            {
                if (testClassSiblingMediator.isTestClassName(testSubClassCandidate.getName()))
                {
                    testClass = testSubClassCandidate;
                    break;
                }
            }
        }

        PsiClass subjectClass =
            testClassSiblingMediator
                .getSubjectClass(
                    testClass,
                    searchScope
                );

        if (subjectClass != null)
        {
            List<PsiMethod> subjectMethods = testMethodSiblingMediator.getSubjectMethods(testMethod, subjectClass);

            if (!subjectMethods.isEmpty())
            {
                String newSubjectName =
                    testMethodSiblingMediator
                        .renameSubjectName(subjectMethods.get(0).getName(), oldTestName, newTestName);

                if (newSubjectName == null)
                {
                    if (!Objects.equals(oldTestName, newTestName))
                    {
                        renames.put(testMethod, newTestName);
                    }
                }
                else
                {
                    subjectMethods
                        .forEach(
                            subjectMethod -> renames.putAll(
                                renameSubjectMethodMapping(subjectMethod, newSubjectName, searchScope)
                            )
                        );
                }
            }
        }

        return renames;
    }

    public void update()
    {
        testClassSiblingMediator = new PatternBasedTestClassSiblingMediator(settings.getTestClassPattern());
        testMethodSiblingMediator =
            new PatternBasedTestMethodSiblingMediator(
                settings.getTestMethodNamePattern(),
                CapitalizationScheme.IF_PREFIXED,
                settings.getTestAnnotations()
            );


    }

    public GlobalSearchScope getSearchScope(PsiElement element, NamedScope scope)
    {
        Module module = ModuleUtilCore.findModuleForPsiElement(element);

        if (module == null)
        {
            return GlobalSearchScope.EMPTY_SCOPE;
        }

        return GlobalSearchScope.moduleWithDependentsScope(module);
    }
}
