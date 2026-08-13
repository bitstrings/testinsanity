package org.bitstrings.idea.plugins.testinsanity.util;

public class IllegalWildcardTestPatternException
    extends TestPatternException
{
    private static final long serialVersionUID = 1L;

    public IllegalWildcardTestPatternException(String message, String pattern)
    {
        super(message, pattern);
    }
}
