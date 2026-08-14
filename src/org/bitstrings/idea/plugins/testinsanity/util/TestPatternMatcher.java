package org.bitstrings.idea.plugins.testinsanity.util;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public final class TestPatternMatcher
{
    private static final Pattern AFFIX_REGEX_PATTERN = Pattern.compile("([^\\|\\(\\)\\+\\*]+)([\\|\\(])?");

    private static final Pattern VALID_CHARS =
        Pattern.compile("[^\\w_\\$\\+\\*\\(\\)\\|\\s'\\-\\.]", Pattern.CASE_INSENSITIVE);

    private static final Pattern ALTERNATION_GROUP = Pattern.compile("\\(([^\\(\\)]*)\\)");

    private static final String GENERATED_WILDCARD_VALUE = "test";

    private static final String[] WILDCARDS = new String[] { "*", "+" };

    private static final String[] WILDCARDS_REGEX = new String[] { ".*?", ".+?" };

    private final String pattern;

    private final String subjectToken;

    private final String prefixPattern;

    private final String prefixPatternRegex;

    private final Pattern prefixValueRegex;

    private final String suffixPattern;

    private final String suffixPatternRegex;

    private final Pattern suffixValueRegex;

    private final Pattern tokenValueRegex;

    private final boolean supportWildcards;

    private final CapitalizationScheme subjectCapitalizationScheme;

    public enum CapitalizationScheme
    {
        UNCHANGED, IF_PREFIXED, ALWAYS
    }

    public TestPatternMatcher(
        String pattern, String subjectToken, boolean supportWildcards, CapitalizationScheme subjectCapitalizationScheme
    )
        throws TestPatternException
    {
        this.pattern = pattern;
        this.subjectToken = subjectToken;
        this.subjectCapitalizationScheme = subjectCapitalizationScheme;
        this.supportWildcards = supportWildcards;
        this.prefixPattern = substringBefore(pattern, subjectToken);
        this.prefixPatternRegex = generateRegexFromPattern(this.prefixPattern);
        this.suffixPattern = substringAfter(pattern, subjectToken);
        this.suffixPatternRegex = generateRegexFromPattern(this.suffixPattern);
        this.prefixValueRegex = Pattern.compile("^(?<prefix>" + this.prefixPatternRegex + ")(?<rest>.+)");
        this.suffixValueRegex = Pattern.compile("(?<suffix>" + this.suffixPatternRegex + ")$");
        this.tokenValueRegex =
            Pattern.compile(
                "^(?<prefix>" + this.prefixPatternRegex + ")"
                    + "(?<tokenValue>.+?)"
                    + "(?<suffix>" + this.suffixPatternRegex + ")$"
            );
    }

    private static List<String> getPatternInvalidChar(String pattern)
    {
        return VALID_CHARS.matcher(pattern).results().map(MatchResult::group).collect(Collectors.toList());
    }

    private static boolean patternContainsWildcard(String pattern)
    {
        return (pattern.indexOf('*') >= 0) || (pattern.indexOf('+') >= 0);
    }

    private boolean patternContainsTokenName()
    {
        return pattern.contains(subjectToken);
    }

    private static String generateRegexFromPattern(String pattern)
    {
        return
            StringUtils.replaceEach(
                AFFIX_REGEX_PATTERN.matcher(pattern).replaceAll("\\\\Q$1\\\\E$2"), WILDCARDS, WILDCARDS_REGEX);
    }

    public void validatePattern()
        throws TestPatternException
    {
        if (!patternContainsTokenName())
        {
            throw new MissingTokenNameTestPatternException("Pattern missing subject token.", pattern);
        }

        if (patternContainsWildcard(prefixPattern))
        {
            throw new IllegalWildcardTestPatternException("Prefix pattern can not contain wildcards.", prefixPattern);
        }

        try
        {
            Pattern.compile(prefixPatternRegex);
        }
        catch (PatternSyntaxException e)
        {
            throw new TestPatternException("Prefix pattern is invalid.", prefixPattern);
        }

        if (!supportWildcards && patternContainsWildcard(suffixPattern))
        {
            throw new IllegalWildcardTestPatternException("Pattern can not contain wildcards.", suffixPattern);
        }

        List<String> invalidChars = getPatternInvalidChar(prefixPattern + suffixPattern);
        if (!invalidChars.isEmpty())
        {
            throw new InvalidCharsTestPatternException("Pattern has Invalid character(s).", pattern, invalidChars);
        }

        try
        {
            Pattern.compile(suffixPatternRegex);
        }
        catch (PatternSyntaxException e)
        {
            throw new TestPatternException("Suffix pattern is invalid.", suffixPattern);
        }
    }

    public boolean matchesPattern(String source)
    {
        if (supportWildcards)
        {
            throw new IllegalStateException("Can't match pattern with wildcards.");
        }

        return tokenValueRegex.matcher(source).matches();
    }

    public String findSubjectName(String testName)
    {
        if (patternContainsWildcard(prefixPattern) || patternContainsWildcard(suffixPattern))
        {
            return null;
        }

        Matcher matcher = tokenValueRegex.matcher(testName);

        if (!matcher.matches())
        {
            return null;
        }

        String subjectName = matcher.group("tokenValue");

        return subjectIsCapitalized(matcher.group("prefix"))
            ? uncapitalize(subjectName)
            : subjectName;
    }

    public TestPatternMatchResult findTestMatch(String test, String subjectCandidate)
    {
        return findTestMatch(test, singletonList(subjectCandidate));
    }

    public TestPatternMatchResult findTestMatch(String test, List<String> subjectCandidates)
    {
        Matcher matcher = prefixValueRegex.matcher(test);

        if (!matcher.matches())
        {
            return TestPatternMatchResult.UNMATCHED;
        }

        String prefixValue = matcher.group("prefix");

        String restValue = matcher.group("rest");

        String foundValue = "";
        String suffixValue = "";

        for (String candidate : subjectCandidates)
        {
            String candidateTestSubject = capitalizeSubject(candidate, prefixValue);

            String candidateSuffixValue = removePrefix(restValue, candidateTestSubject);
            if (
                (candidateSuffixValue.length() < restValue.length()) && (candidateTestSubject.length() > foundValue
                    .length())
            )
            {
                foundValue = candidate;
                suffixValue = candidateSuffixValue;
            }
        }

        if (!suffixValueRegex.matcher(suffixValue).matches() || foundValue.isEmpty())
        {
            return TestPatternMatchResult.UNMATCHED;
        }

        return new TestPatternMatchResult(prefixValue, foundValue, suffixValue);
    }

    public String renameTest(String test, String oldSubject, String newSubject)
    {
        TestPatternMatchResult sourceParts = findTestMatch(test, oldSubject);

        if (!sourceParts.isMatched())
        {
            return test;
        }

        return (sourceParts.getPrefix()
            + capitalizeSubject(newSubject, sourceParts.getPrefix())
            + sourceParts.getSuffix());
    }

    public String generateTestName(String subjectName)
    {
        String prefix = resolveLiteral(prefixPattern);

        return prefix + capitalizeSubject(subjectName, prefix) + resolveLiteral(suffixPattern);
    }

    private String capitalizeSubject(String subjectName, String prefix)
    {
        return subjectIsCapitalized(prefix) ? capitalize(subjectName) : subjectName;
    }

    private boolean subjectIsCapitalized(String prefix)
    {
        return (subjectCapitalizationScheme == CapitalizationScheme.ALWAYS)
            || ((subjectCapitalizationScheme == CapitalizationScheme.IF_PREFIXED) && !prefix.isEmpty());
    }

    private static String resolveLiteral(String pattern)
    {
        Matcher matcher = ALTERNATION_GROUP.matcher(pattern);

        StringBuilder resolved = new StringBuilder();

        while (matcher.find())
        {
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(resolveAlternation(matcher.group(1))));
        }

        matcher.appendTail(resolved);

        return resolved.toString().replace("*", "").replace("+", GENERATED_WILDCARD_VALUE);
    }

    private static String removePrefix(String value, String prefix)
    {
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static String removeSuffix(String value, String suffix)
    {
        return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
    }

    private static String resolveAlternation(String alternation)
    {
        String[] alternatives = alternation.split("\\|", -1);

        for (String alternative : alternatives)
        {
            if (alternative.isEmpty())
            {
                return "";
            }
        }

        return alternatives[0];
    }

    public String renameSubject(String oldSubject, String oldTest, String newTest)
    {
        TestPatternMatchResult testParts = findTestMatch(oldTest, oldSubject);

        if (!testParts.isMatched())
        {
            return newTest;
        }

        if (!newTest.startsWith(testParts.getPrefix()) || !newTest.endsWith(testParts.getSuffix()))
        {
            return null;
        }

        String newSubject =
            removeSuffix(removePrefix(newTest, testParts.getPrefix()), testParts.getSuffix());

        return subjectIsCapitalized(testParts.getPrefix())
            ? uncapitalize(newSubject)
            : newSubject;
    }
}
