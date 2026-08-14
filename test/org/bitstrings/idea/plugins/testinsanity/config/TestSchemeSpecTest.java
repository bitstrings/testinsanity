package org.bitstrings.idea.plugins.testinsanity.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class TestSchemeSpecTest
{
    @Test
    public void migrate_oneClassPattern_yieldsOneSchemeNamedDefault()
    {
        List<TestSchemeSpec> migrated =
            TestSchemeSpec.migrate(List.of("${className}Test"), List.of("a${subjectName}*"));

        assertEquals(1, migrated.size());
        assertEquals(TestSchemeSpec.DEFAULT_NAME, migrated.get(0).name);
        assertEquals("${className}Test", migrated.get(0).testClass);
    }

    @Test
    public void migrate_severalMethodPatterns_keepsAllOfThemInEveryScheme()
    {
        List<TestSchemeSpec> migrated =
            TestSchemeSpec
                .migrate(
                    List.of("${className}Test", "${className}IT"),
                    List.of("a${subjectName}*", "b${subjectName}*"));

        assertEquals(2, migrated.size());
        assertEquals(List.of("a${subjectName}*", "b${subjectName}*"), migrated.get(0).testMethods);
        assertEquals(List.of("a${subjectName}*", "b${subjectName}*"), migrated.get(1).testMethods);
    }

    @Test
    public void migrate_severalClassPatterns_namesTheSchemesInOrder()
    {
        List<TestSchemeSpec> migrated =
            TestSchemeSpec
                .migrate(List.of("${className}Test", "${className}IT"), List.of("a${subjectName}*"));

        assertEquals(TestSchemeSpec.DEFAULT_NAME, migrated.get(0).name);
        assertEquals("scheme2", migrated.get(1).name);
    }

    @Test
    public void copy_aScheme_equalsTheOriginal()
    {
        TestSchemeSpec scheme = new TestSchemeSpec("unit", "${className}Test", List.of("test${subjectName}*"));

        assertEquals(scheme, scheme.copy());
        assertEquals(scheme.hashCode(), scheme.copy().hashCode());
    }

    @Test
    public void equals_differentName_isNotEqual()
    {
        TestSchemeSpec unit = new TestSchemeSpec("unit", "${className}Test", List.of("test${subjectName}*"));
        TestSchemeSpec integration = new TestSchemeSpec("it", "${className}Test", List.of("test${subjectName}*"));

        assertNotEquals(unit, integration);
    }

    @Test
    public void isComplete_missingClassPattern_isIncomplete()
    {
        assertFalse(new TestSchemeSpec("unit", "", List.of("${subjectName}*")).isComplete());
    }

    @Test
    public void isComplete_noMethodPattern_isIncomplete()
    {
        assertFalse(new TestSchemeSpec("unit", "${className}Test", List.of()).isComplete());
    }

    @Test
    public void isComplete_nameAndBothPatterns_isComplete()
    {
        assertTrue(new TestSchemeSpec("unit", "${className}Test", List.of("${subjectName}*")).isComplete());
    }
}
