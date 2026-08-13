package org.bitstrings.idea.plugins.testinsanity;

import static com.intellij.codeInsight.AnnotationUtil.checkAnnotatedUsingPatterns;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bitstrings.idea.plugins.testinsanity.util.TestPatternException;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatchResult;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher.CapitalizationScheme;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

public class PatternBasedTestMethodSiblingMediator
    implements TestMethodSiblingMediator
{
    public static final String DEFAULT_SUBJECT_NAME_TOKEN = "${subjectName}";

    public static final String DEFAULT_METHOD_NAME_PATTERN = "(test|)${subjectName}*";

    private final String testMethodNamePattern;

    private final Set<String> testMethodAnnotations;

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
        this.includeInheritedMethods = includeInheritedMethods;
        this.includeNestedClasses = includeNestedClasses;
    }

    public String getTestMethodNamePattern()
    {
        return testMethodNamePattern;
    }

    public Set<String> getTestMethodAnnotations()
    {
        return testMethodAnnotations;
    }

    @Override
    public boolean checkMethodAnnotation(PsiMethod targetMethod, boolean failOnEmpty)
    {
        return
            ((testMethodAnnotations.isEmpty() && !failOnEmpty)
                || checkAnnotatedUsingPatterns(targetMethod, testMethodAnnotations));
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

        for (PsiClass testClass : testClasses)
        {
            collectTestMethods(subjectMethod, testClass, testMethods);
        }

        return new ArrayList<>(testMethods);
    }

    protected void collectTestMethods(PsiMethod subjectMethod, PsiClass testClass, Set<PsiMethod> testMethods)
    {
        for (PsiMethod method : (includeInheritedMethods ? testClass.getAllMethods() : testClass.getMethods()))
        {
            if (checkMethodAnnotation(method, false) && matchesTestName(method.getName(), subjectMethod.getName()))
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
            collectTestMethods(subjectMethod, nestedClass, testMethods);
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
            testMethodPatternMatcher.findTestMatch(
                testMethod.getName(),
                Arrays.stream(
                    includeInheritedMethods ? subjectClass.getAllMethods() : subjectClass.getMethods()
                ).map(PsiMethod::getName).collect(toList())
            );

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

    @Override
    public String generateTestName(String subjectName)
    {
        return testMethodPatternMatcher.generateTestName(subjectName);
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
