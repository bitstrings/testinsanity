package org.bitstrings.idea.plugins.testinsanity.util;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;

public class InvalidCharsTestPatternException
    extends TestPatternException
{
    private static final long serialVersionUID = 1L;

    private final ArrayList<String> invalidChars;

    public InvalidCharsTestPatternException(String message, String pattern, List<String> invalidChars)
    {
        super(message, pattern);

        this.invalidChars = new ArrayList<>(invalidChars);
    }

    public List<String> getInvalidChars()
    {
        return unmodifiableList(invalidChars);
    }
}
