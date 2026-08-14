package org.bitstrings.idea.plugins.testinsanity;

import static com.intellij.codeInsight.MetaAnnotationUtil.isMetaAnnotatedInHierarchy;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bitstrings.idea.plugins.testinsanity.util.TestAnnotationPattern;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternException;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatchResult;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher.CapitalizationScheme;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

public class PatternBasedTestMethodSiblingMediator
    implements TestMethodSiblingMediator
{
    public static final String DEFAULT_SUBJECT_NAME_TOKEN = "${subjectName}";

    public static final String DEFAULT_METHOD_NAME_PATTERN = "(test|)${subjectName}*";

    public static final String PREFIXED_METHOD_NAME_PATTERN = "test${subjectName}*";

    public static final String SUFFIXED_METHOD_NAME_PATTERN = "${subjectName}_+";

    public static final String EXACT_METHOD_NAME_PATTERN = DEFAULT_SUBJECT_NAME_TOKEN;

    private final String testMethodNamePattern;

    private final Set<String> testMethodAnnotations;

    private final Set<String> annotationNames;

    private final List<String> annotationPackages;

    private final TestPatternMatcher testMethodPatternMatcher;

    private final boolean includeInheritedMethods;

    private final boolean includeNestedClasses;

    public PatternBasedTestMethodSiblingMediator()
    {
        this(DEFAULT_METHOD_NAME_PATTERN, CapitalizationScheme.IF_PREFIXED, emptySet(), true, true);
    }

    public PatternBasedTestMethodSiblingMediator(
        String testMethodNamePattern,
        CapitalizationScheme capitalizeSubjectNameScheme,
        Set<String> testMethodAnnotations,
        boolean includeInheritedMethods,
        boolean includeNestedClasses
    )
        throws TestPatternException
    {
        this.testMethodNamePattern = testMethodNamePattern;
        this.testMethodPatternMatcher =
            new TestPatternMatcher(
                this.testMethodNamePattern,
                DEFAULT_SUBJECT_NAME_TOKEN,
                true,
                capitalizeSubjectNameScheme
            );
        this.testMethodAnnotations = testMethodAnnotations;

        Set<String> names = new LinkedHashSet<>();
        List<String> packages = new ArrayList<>();

        for (String annotation : testMethodAnnotations)
        {
            if (annotation.endsWith(TestAnnotationPattern.PACKAGE_WILDCARD_SUFFIX))
            {
                packages.add(annotation.substring(0, annotation.length() - 1));
            }
            else
            {
                names.add(annotation);
            }
        }

        this.annotationNames = Set.copyOf(names);
        this.annotationPackages = List.copyOf(packages);

        this.includeInheritedMethods = includeInheritedMethods;
        this.includeNestedClasses = includeNestedClasses;
    }

    @Override
    public boolean checkMethodAnnotation(PsiMethod targetMethod, boolean failOnEmpty)
    {
        if (testMethodAnnotations.isEmpty())
        {
            return !failOnEmpty;
        }

        return isAnnotatedFromPackage(targetMethod)
            || (!annotationNames.isEmpty() && isMetaAnnotatedInHierarchy(targetMethod, annotationNames));
    }

    private boolean isAnnotatedFromPackage(PsiMethod targetMethod)
    {
        if (annotationPackages.isEmpty())
        {
            return false;
        }

        for (PsiAnnotation annotation : targetMethod.getModifierList().getAnnotations())
        {
            String qualifiedName = annotation.getQualifiedName();

            if (qualifiedName == null)
            {
                continue;
            }

            for (String annotationPackage : annotationPackages)
            {
                if (qualifiedName.startsWith(annotationPackage))
                {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void validatePattern()
        throws TestPatternException
    {
        testMethodPatternMatcher.validatePattern();
    }

    @Override
    public List<PsiMethod> getTestMethods(PsiMethod subjectMethod, List<PsiClass> testClasses)
    {
        Set<PsiMethod> testMethods = new LinkedHashSet<>();

        List<String> subjectCandidates = subjectCandidates(subjectMethod);

        for (PsiClass testClass : testClasses)
        {
            collectTestMethods(subjectMethod, subjectCandidates, testClass, testMethods);
        }

        return new ArrayList<>(testMethods);
    }

    protected void collectTestMethods(
        PsiMethod subjectMethod, List<String> subjectCandidates, PsiClass testClass, Set<PsiMethod> testMethods
    )
    {
        for (PsiMethod method : (includeInheritedMethods ? testClass.getAllMethods() : testClass.getMethods()))
        {
            if (checkMethodAnnotation(method, false) && isTestOf(method.getName(), subjectMethod, subjectCandidates))
            {
                testMethods.add(method);
            }
        }

        if (!includeNestedClasses)
        {
            return;
        }

        for (PsiClass nestedClass : testClass.getInnerClasses())
        {
            collectTestMethods(subjectMethod, subjectCandidates, nestedClass, testMethods);
        }
    }

    @Override
    public List<PsiMethod> getSubjectMethods(PsiMethod testMethod, PsiClass subjectClass)
    {
        if (!checkMethodAnnotation(testMethod, false))
        {
            return emptyList();
        }

        TestPatternMatchResult testNameParts =
            testMethodPatternMatcher.findTestMatch(testMethod.getName(), subjectMethodNames(subjectClass));

        if (!testNameParts.isMatched())
        {
            return emptyList();
        }

        PsiMethod[] subjectMethods =
            subjectClass.findMethodsByName(testNameParts.getSubject(), includeInheritedMethods);

        return subjectMethods == null
            ? emptyList()
            : asList(subjectMethods);
    }

    @Override
    public boolean matchesTestName(String testName, String subjectName)
    {
        return testMethodPatternMatcher.findTestMatch(testName, subjectName).isMatched();
    }

    private boolean isTestOf(String testName, PsiMethod subjectMethod, List<String> subjectCandidates)
    {
        return subjectMethod
            .getName()
            .equals(testMethodPatternMatcher.findTestMatch(testName, subjectCandidates).getSubject());
    }

    private List<String> subjectCandidates(PsiMethod subjectMethod)
    {
        PsiClass subjectClass = subjectMethod.getContainingClass();

        return (subjectClass == null)
            ? List.of(subjectMethod.getName())
            : subjectMethodNames(subjectClass);
    }

    private List<String> subjectMethodNames(PsiClass subjectClass)
    {
        return Arrays
            .stream(includeInheritedMethods ? subjectClass.getAllMethods() : subjectClass.getMethods())
            .map(PsiMethod::getName)
            .distinct()
            .collect(toList());
    }

    @Override
    public String generateTestName(String subjectName)
    {
        return testMethodPatternMatcher.generateTestName(subjectName);
    }

    @Override
    public String findSubjectName(String testName)
    {
        return testMethodPatternMatcher.findSubjectName(testName);
    }

    @Override
    public String renameTestName(String oldTestName, String oldSubjectName, String newSubjectName)
    {
        return testMethodPatternMatcher.renameTest(oldTestName, oldSubjectName, newSubjectName);
    }

    @Override
    public String renameSubjectName(String oldSubjectName, String oldTestName, String newTestName)
    {
        return testMethodPatternMatcher.renameSubject(oldSubjectName, oldTestName, newTestName);
    }
}
