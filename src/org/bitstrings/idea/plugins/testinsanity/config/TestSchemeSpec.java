package org.bitstrings.idea.plugins.testinsanity.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public final class TestSchemeSpec
{
    public static final String DEFAULT_NAME = "default";

    public String name;

    public String testClass;

    public final List<String> testMethods = new ArrayList<>();

    public TestSchemeSpec()
    {
    }

    public TestSchemeSpec(String name, String testClass, List<String> testMethods)
    {
        this.name = name;
        this.testClass = testClass;
        this.testMethods.addAll(testMethods);
    }

    public static List<TestSchemeSpec> migrate(List<String> testClassPatterns, List<String> testMethodPatterns)
    {
        List<TestSchemeSpec> migrated = new ArrayList<>();

        for (int index = 0; index < testClassPatterns.size(); index++)
        {
            migrated.add(
                new TestSchemeSpec(nameFor(index), testClassPatterns.get(index), testMethodPatterns));
        }

        return migrated;
    }

    private static String nameFor(int index)
    {
        return (index == 0) ? DEFAULT_NAME : ("scheme" + (index + 1));
    }

    public boolean isComplete()
    {
        return StringUtils.isNotBlank(name) && StringUtils.isNotBlank(testClass) && !testMethods.isEmpty();
    }

    public TestSchemeSpec copy()
    {
        return new TestSchemeSpec(name, testClass, testMethods);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof TestSchemeSpec))
        {
            return false;
        }

        TestSchemeSpec scheme = (TestSchemeSpec) other;

        return Objects.equals(name, scheme.name)
            && Objects.equals(testClass, scheme.testClass)
            && testMethods.equals(scheme.testMethods);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, testClass, testMethods);
    }

    @Override
    public String toString()
    {
        return name + " " + testClass + " " + testMethods;
    }
}
