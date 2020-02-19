package org.bitstrings.idea.plugins.testinsanity.util;

import java.util.List;

public class InvalidCharsTestPatternException
    extends TestPatternException
{
    private final List<String> invalidChars;

    public InvalidCharsTestPatternException(String message, String pattern, List<String> invalidChars)
    {
        super(message, pattern);

        this.invalidChars = invalidChars;
    }

    public List<String> getInvalidChars()
    {
        return invalidChars;
    }
}
