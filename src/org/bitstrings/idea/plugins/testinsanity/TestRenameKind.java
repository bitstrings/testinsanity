package org.bitstrings.idea.plugins.testinsanity;

enum TestRenameKind
{
    CLASS("testinsanity.renamer.class"),
    METHOD("testinsanity.renamer.method");

    private final String bundleKeyPrefix;

    TestRenameKind(String bundleKeyPrefix)
    {
        this.bundleKeyPrefix = bundleKeyPrefix;
    }

    String message(String bundleKeySuffix)
    {
        return TestInsanityBundle.message(bundleKeyPrefix + "." + bundleKeySuffix);
    }
}
