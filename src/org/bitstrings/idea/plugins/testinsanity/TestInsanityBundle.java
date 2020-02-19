package org.bitstrings.idea.plugins.testinsanity;

import java.util.ResourceBundle;

import org.jetbrains.annotations.NonNls;

public class TestInsanityBundle
{
    @NonNls
    private static final String BUNDLE = "messages.TestInsanityBundle";

    private static final TestInsanityBundle INSTANCE = new TestInsanityBundle();

    private final ResourceBundle resourceBundle;

    protected TestInsanityBundle()
    {
        resourceBundle = ResourceBundle.getBundle(BUNDLE);
    }

    public static String message(String key)
    {
        return INSTANCE.resourceBundle.getString(key);
    }

    public static boolean containsKey(String key)
    {
        return INSTANCE.resourceBundle.containsKey(key);
    }
}
