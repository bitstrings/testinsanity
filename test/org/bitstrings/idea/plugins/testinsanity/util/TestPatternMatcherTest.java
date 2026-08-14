package org.bitstrings.idea.plugins.testinsanity.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher.CapitalizationScheme;
import org.junit.Test;

public class TestPatternMatcherTest
{
    private static final String CLASS_TOKEN = "${className}";

    private static final String SUBJECT_TOKEN = "${subjectName}";

    @Test
    public void generateTestName_everyShippedMethodPattern_producesANameThatPatternMatches()
    {
        List<String> methodPatterns =
            List.of(
                "(test|)${subjectName}*", "test${subjectName}*", "${subjectName}_+", "${subjectName} *",
                "(test|)${subjectName}(_+_+|_factory|)", "${subjectName}(_+|)", "(test|)${subjectName}(_+|)",
                "test${subjectName}(_+|)", "test${subjectName}_+");

        for (String methodPattern : methodPatterns)
        {
            TestPatternMatcher matcher = methodMatcher(methodPattern);

            String generated = matcher.generateTestName("isDarkColor");

            assertTrue(
                methodPattern + " generated " + generated + " which it does not match itself",
                matcher.findTestMatch(generated, "isDarkColor").isMatched());
        }
    }

    @Test
    public void findTestMatch_prefixedTestNameWithSuffix_findsTheSubject()
    {
        TestPatternMatcher matcher = methodMatcher("(test|)" + SUBJECT_TOKEN + "*");

        TestPatternMatchResult match = matcher.findTestMatch("testIsDarkColor_whenBlack", "isDarkColor");

        assertEquals("isDarkColor", match.getSubject());
        assertEquals("test", match.getPrefix());
    }

    @Test
    public void findTestMatch_unrelatedName_isNotMatched()
    {
        TestPatternMatcher matcher = methodMatcher("test" + SUBJECT_TOKEN + "*");

        TestPatternMatchResult match = matcher.findTestMatch("checkDarkColor", "isDarkColor");

        assertFalse(match.isMatched());
    }

    @Test
    public void renameTest_subjectRenamed_keepsPrefixAndSuffix()
    {
        TestPatternMatcher matcher = methodMatcher("(test|)" + SUBJECT_TOKEN + "*");

        String renamed = matcher.renameTest("testIsDarkColor_whenBlack", "isDarkColor", "isPale");

        assertEquals("testIsPale_whenBlack", renamed);
    }

    @Test
    public void renameTest_nonMatchingTestName_isLeftAlone()
    {
        TestPatternMatcher matcher = methodMatcher("test" + SUBJECT_TOKEN);

        String renamed = matcher.renameTest("checkDarkColor", "isDarkColor", "isPale");

        assertEquals("checkDarkColor", renamed);
    }

    @Test
    public void renameSubject_testRenamed_recoversTheNewSubjectName()
    {
        TestPatternMatcher matcher = methodMatcher("(test|)" + SUBJECT_TOKEN + "*");

        String renamed = matcher.renameSubject("isDarkColor", "testIsDarkColor_whenBlack", "testIsPale_whenBlack");

        assertEquals("isPale", renamed);
    }

    @Test
    public void renameSubject_newTestNameBreaksThePattern_returnsNull()
    {
        TestPatternMatcher matcher = methodMatcher("test" + SUBJECT_TOKEN);

        String renamed = matcher.renameSubject("isDarkColor", "testIsDarkColor", "checkIsPale");

        assertNull(renamed);
    }

    @Test
    public void findSubjectName_classPatternWithoutWildcard_recoversTheSubjectName()
    {
        TestPatternMatcher matcher = classMatcher(CLASS_TOKEN + "IT");

        String subjectName = matcher.findSubjectName("ColorIT");

        assertEquals("Color", subjectName);
    }

    @Test
    public void findSubjectName_prefixedMethodPattern_uncapitalizesTheSubjectName()
    {
        TestPatternMatcher matcher = methodMatcher("test" + SUBJECT_TOKEN);

        String subjectName = matcher.findSubjectName("testIsDarkColor");

        assertEquals("isDarkColor", subjectName);
    }

    @Test
    public void findSubjectName_nameThatDoesNotMatchThePattern_returnsNull()
    {
        TestPatternMatcher matcher = classMatcher(CLASS_TOKEN + "IT");

        String subjectName = matcher.findSubjectName("ColorTest");

        assertNull(subjectName);
    }

    @Test
    public void findSubjectName_wildcardSuffixMakesTheSubjectAmbiguous_returnsNull()
    {
        TestPatternMatcher matcher = methodMatcher("(test|)" + SUBJECT_TOKEN + "*");

        String subjectName = matcher.findSubjectName("testIsDarkColor_whenBlack");

        assertNull(subjectName);
    }

    @Test
    public void validatePattern_patternWithoutTheSubjectToken_isRejected()
    {
        assertRejected(methodMatcher("testEverything"));
    }

    @Test
    public void validatePattern_wildcardBeforeTheSubjectToken_isRejected()
    {
        assertRejected(methodMatcher("*" + SUBJECT_TOKEN));
    }

    @Test
    public void validatePattern_wildcardInAClassPattern_isRejected()
    {
        assertRejected(classMatcher(CLASS_TOKEN + "*"));
    }

    @Test
    public void matchesPattern_classPatternAndAMatchingName_isMatched()
    {
        TestPatternMatcher matcher = classMatcher(CLASS_TOKEN + "Test");

        assertTrue(matcher.matchesPattern("ColorTest"));
        assertFalse(matcher.matchesPattern("Color"));
    }

    private static void assertRejected(TestPatternMatcher matcher)
    {
        try
        {
            matcher.validatePattern();

            fail("The pattern was accepted but must be rejected");
        }
        catch (TestPatternException ignored)
        {
            return;
        }
    }

    private static TestPatternMatcher methodMatcher(String methodPattern)
    {
        return new TestPatternMatcher(methodPattern, SUBJECT_TOKEN, true, CapitalizationScheme.IF_PREFIXED);
    }

    private static TestPatternMatcher classMatcher(String classPattern)
    {
        return new TestPatternMatcher(classPattern, CLASS_TOKEN, false, CapitalizationScheme.UNCHANGED);
    }
}
