package org.bitstrings.idea.plugins.testinsanity.util;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

public final class TestDisplayNames
{
    public static final String DISPLAY_NAME_FQN = "org.junit.jupiter.api.DisplayName";

    private TestDisplayNames()
    {
    }

    public static String humanize(String name)
    {
        return StringUtils
            .normalizeSpace(
                StringUtils.join(
                    StringUtils.splitByCharacterTypeCamelCase(StringUtils.replaceChars(name, '_', ' ')), ' '))
            .toLowerCase(Locale.ROOT);
    }
}
