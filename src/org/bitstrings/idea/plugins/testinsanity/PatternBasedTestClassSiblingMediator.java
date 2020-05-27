package org.bitstrings.idea.plugins.testinsanity;

import static org.apache.commons.lang.StringUtils.substringBetween;

import java.util.LinkedList;
import java.util.List;

import org.bitstrings.idea.plugins.testinsanity.util.TestPatternException;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatchResult;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher.CapitalizationScheme;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.psi.util.PsiUtil;

public class PatternBasedTestClassSiblingMediator
    implements TestClassSiblingMediator
{
    public static final String DEFAULT_TEST_CLASS_NAME_TOKEN = "${className}";

    public static final String DEFAULT_TEST_CLASS_NAME_PATTERN = DEFAULT_TEST_CLASS_NAME_TOKEN + "Test";

    private final String testClassNamePattern;

    private final TestPatternMatcher testClassPatternMatcher;

    private final boolean includeAbstracts;

    public PatternBasedTestClassSiblingMediator()
    {
        this(DEFAULT_TEST_CLASS_NAME_PATTERN, false);
    }

    public PatternBasedTestClassSiblingMediator(String testClassNamePattern, boolean includeAbstracts)
        throws TestPatternException
    {
        this.testClassNamePattern = testClassNamePattern;

        this.testClassPatternMatcher =
            new TestPatternMatcher(
                this.testClassNamePattern,
                DEFAULT_TEST_CLASS_NAME_TOKEN,
                false,
                CapitalizationScheme.UNCHANGED
            );

        this.includeAbstracts = includeAbstracts;
    }

    public String getTestClassNamePattern()
    {
        return testClassNamePattern;
    }

    public TestPatternMatcher getTestClassPatternMatcher()
    {
        return testClassPatternMatcher;
    }

    @Override
    public void validatePattern()
        throws TestPatternException
    {
        testClassPatternMatcher.validatePattern();
    }

    @Override
    public List<PsiClass> getTestClasses(PsiClass subjectClass, GlobalSearchScope searchScope)
    {
        Project project = subjectClass.getProject();

        List<PsiClass> matchedClasses = new LinkedList<>();

        PsiPackage classPackage = JavaPsiFacade.getInstance(project).findPackage(PsiUtil.getPackageName(subjectClass));

        if (classPackage != null)
        {
            PsiClass[] candidateTestClasses =
                classPackage.getClasses(searchScope.intersectWith(GlobalSearchScopes.projectTestScope(project)));

            for (PsiClass candidateTestClass : candidateTestClasses)
            {
                if (
                    (includeAbstracts
                        || !(candidateTestClass.isInterface() || PsiUtil.isAbstractClass(candidateTestClass)))
                        && testClassPatternMatcher
                            .findTestMatch(candidateTestClass.getName(), subjectClass.getName())
                            .isMatched()
                )
                {
                    matchedClasses.add(candidateTestClass);
                }
            }
        }

        return matchedClasses;
    }

    @Override
    public PsiClass getSubjectClass(PsiClass testClass, GlobalSearchScope searchScope)
    {
        Project project = testClass.getProject();

        searchScope = searchScope.intersectWith(GlobalSearchScopes.projectProductionScope(project));

        PsiPackage testClassPackage = JavaPsiFacade.getInstance(project).findPackage(PsiUtil.getPackageName(testClass));

        if (testClassPackage != null)
        {
            PsiClass[] subjectCandidateClasses = testClassPackage.getClasses(searchScope);

            for (PsiClass subjectCandidateClass : subjectCandidateClasses)
            {
                if (
                    testClassPatternMatcher
                        .findTestMatch(testClass.getName(), subjectCandidateClass.getName())
                        .isMatched()
                )
                {
                    return subjectCandidateClass;
                }
            }
        }

        return null;
    }

    @Override
    public String renameTestName(String testName, String oldSubjectName, String newSubjectName)
    {
        return testClassPatternMatcher.renameTest(testName, oldSubjectName, newSubjectName);
    }

    @Override
    public String renameSubjectName(String subjectName, String oldTestName, String newTestName)
    {
        TestPatternMatchResult testNameParts =
            testClassPatternMatcher.findTestMatch(oldTestName, subjectName);

        if (!testNameParts.isMatched())
        {
            return newTestName;
        }

        return substringBetween(newTestName, testNameParts.getPrefix(), testNameParts.getSuffix());
    }

    @Override
    public boolean isTestClass(PsiClass candidateTestClass)
    {
        return (includeAbstracts || !(candidateTestClass.isInterface() || PsiUtil.isAbstractClass(candidateTestClass)))
            && isTestClassName(candidateTestClass.getName());
    }

    protected boolean isTestClassName(String targetClassName)
    {
        return testClassPatternMatcher.matchesPattern(targetClassName);
    }
}
