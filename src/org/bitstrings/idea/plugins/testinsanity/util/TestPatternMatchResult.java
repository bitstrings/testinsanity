package org.bitstrings.idea.plugins.testinsanity.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public final class TestPatternMatchResult
{
    public static final TestPatternMatchResult UNMATCHED = new TestPatternMatchResult("", "", "");

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

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("prefix", prefix)
            .append("subject", subject)
            .append("suffix", suffix)
            .append("matched", matched)
            .toString();
    }
}
