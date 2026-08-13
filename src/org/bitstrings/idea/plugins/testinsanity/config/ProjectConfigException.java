package org.bitstrings.idea.plugins.testinsanity.config;

public class ProjectConfigException
    extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public ProjectConfigException(String message)
    {
        super(message);
    }

    public ProjectConfigException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
