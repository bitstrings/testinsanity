package org.bitstrings.idea.plugins.testinsanity;

import static com.intellij.codeInsight.AnnotationUtil.checkAnnotatedUsingPatterns;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.LinkedList;
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

    public PatternBasedTestMethodSiblingMediator()
    {
        this(DEFAULT_METHOD_NAME_PATTERN, CapitalizationScheme.IF_PREFIXED, emptySet());
    }

    public PatternBasedTestMethodSiblingMediator(
        String testMethodNamePattern,
        CapitalizationScheme capitalizeSubjectNameScheme,
        Set<String> testMethodAnnotations
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
        List<PsiMethod> testMethods = new LinkedList<>();

        testClasses
            .forEach(
                testClass -> Arrays.stream(testClass.getAllMethods())
                    .forEach(
                        method ->
                        {
                            if (
                                checkMethodAnnotation(method, false)
                                    && (testMethodPatternMatcher.findTestMatch(
                                            method.getName(), subjectMethod.getName()).isMatched())
                            )
                            {
                                testMethods.add(method);
                            }
                        }
                    )
            );

        return testMethods;
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
                Arrays.stream(subjectClass.getAllMethods()).map(PsiMethod::getName).collect(toList())
            );

        if (!testNameParts.isMatched())
        {
            return emptyList();
        }

        PsiMethod[] subjectMethods = subjectClass.findMethodsByName(testNameParts.getSubject(), true);

        return subjectMethods == null
            ? emptyList()
            : asList(subjectMethods);
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
