package org.bitstrings.idea.plugins.testinsanity.util;

public class MissingTokenNameTestPatternException
    extends TestPatternException
{
    private static final long serialVersionUID = 1L;

    public MissingTokenNameTestPatternException(String message, String pattern)
    {
        super(message, pattern);
    }
}
