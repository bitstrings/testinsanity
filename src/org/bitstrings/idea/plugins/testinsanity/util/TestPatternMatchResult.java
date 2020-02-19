package org.bitstrings.idea.plugins.testinsanity.util;

import org.apache.commons.lang.StringUtils;

public final class TestPatternMatchResult
{
    public static final TestPatternMatchResult INVALID = new TestPatternMatchResult("", "", "");

    private final String prefix;

    private final String subject;

    private final String suffix;

    private final boolean matched;

    public TestPatternMatchResult(String prefix, String subject, String suffix)
    {
        this.prefix = prefix;
        this.subject = subject;
        this.suffix = suffix;

        this.matched = !StringUtils.isEmpty(this.subject);
    }

    public String getPrefix()
    {
        return prefix;
    }

    public String getSuffix()
    {
        return suffix;
    }

    public String getSubject()
    {
        return subject;
    }

    public boolean isMatched()
    {
        return matched;
    }
}
