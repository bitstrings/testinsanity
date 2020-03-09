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

    private static final Pattern VALID_CHARS = Pattern.compile("[^\\w_\\$\\+\\*\\(\\)\\|]", Pattern.CASE_INSENSITIVE);

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
    {
        this(pattern, subjectToken, supportWildcards, subjectCapitalizationScheme, false);
    }

    public TestPatternMatcher(
        String pattern, String subjectToken, boolean supportWildcards, CapitalizationScheme subjectCapitalizationScheme,
        boolean validate
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

        if (validate)
        {
            validatePattern();
        }
    }

    public String getSubjectToken()
    {
        return subjectToken;
    }

    public String getPrefixPattern()
    {
        return prefixPattern;
    }

    public String getPrefixPatternRegex()
    {
        return prefixPatternRegex;
    }

    public String getSuffixPattern()
    {
        return suffixPattern;
    }

    public String getSuffixPatternRegex()
    {
        return suffixPatternRegex;
    }

    public List<String> getPatternInvalidChar(String pattern)
    {
        return VALID_CHARS.matcher(pattern).results().map(MatchResult::group).collect(Collectors.toList());
    }

    public boolean patternContainsWildcard(String pattern)
    {
        return StringUtils.containsAny(pattern, "+", "*");
    }

    public boolean patternContainsTokenName()
    {
        return pattern.contains(subjectToken);
    }

    public CapitalizationScheme getSubjectCapitalizationScheme()
    {
        return subjectCapitalizationScheme;
    }

    public static String generateRegexFromPattern(String pattern)
    {
        return
            StringUtils.replaceEach(
                AFFIX_REGEX_PATTERN.matcher(pattern).replaceAll("\\\\Q$1\\\\E$2"),
                new String[] { "*", "+" },
                new String[] { ".*?", ".+?" }
            );
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
            String candidateTestSubject =
                (subjectCapitalizationScheme == CapitalizationScheme.ALWAYS)
                    || ((subjectCapitalizationScheme == CapitalizationScheme.IF_PREFIXED) && !prefixValue.isEmpty())
                        ? capitalize(candidate)
                        : candidate;

            String candidateSuffixValue = StringUtils.removeStart(restValue, candidateTestSubject);
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

        if (
            (subjectCapitalizationScheme == CapitalizationScheme.ALWAYS)
                || ((subjectCapitalizationScheme == CapitalizationScheme.IF_PREFIXED)
                    && !sourceParts.getPrefix().isEmpty())
        )
        {
            newSubject = capitalize(newSubject);
        }

        return (sourceParts.getPrefix() + newSubject + sourceParts.getSuffix());
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
            StringUtils.removeEnd(StringUtils.removeStart(newTest, testParts.getPrefix()), testParts.getSuffix());

        if (
            (subjectCapitalizationScheme == CapitalizationScheme.ALWAYS)
                || ((subjectCapitalizationScheme == CapitalizationScheme.IF_PREFIXED)
                    && !testParts.getPrefix().isEmpty())
        )
        {
            newSubject = uncapitalize(newSubject);
        }

        return newSubject;
    }
}
