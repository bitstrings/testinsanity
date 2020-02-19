package org.bitstrings.idea.plugins.testinsanity.util;

public class MissingTokenNameTestPatternException
    extends TestPatternException
{
    public MissingTokenNameTestPatternException(String message, String pattern)
    {
        super(message, pattern);
    }
}
