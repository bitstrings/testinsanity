package org.bitstrings.idea.plugins.testinsanity.util;

public class TestPatternException
    extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    private final String pattern;

    public TestPatternException(String message, String pattern)
    {
        super(message);

        this.pattern = pattern;
    }

    public String getPattern()
    {
        return pattern;
    }
}
