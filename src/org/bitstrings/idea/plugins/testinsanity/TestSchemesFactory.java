package org.bitstrings.idea.plugins.testinsanity;

import java.util.ArrayList;
import java.util.List;

import org.bitstrings.idea.plugins.testinsanity.config.TestInsanityConfiguration;
import org.bitstrings.idea.plugins.testinsanity.config.TestSchemeSpec;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternException;

public final class TestSchemesFactory
{
    private final TestInsanityConfiguration configuration;

    public TestSchemesFactory(TestInsanityConfiguration configuration)
    {
        this.configuration = configuration;
    }

    public TestSchemes create()
        throws TestPatternException
    {
        List<TestScheme> schemes = new ArrayList<>();

        for (TestSchemeSpec spec : configuration.getSchemes())
        {
            schemes.add(scheme(spec));
        }

        return new TestSchemes(schemes);
    }

    public TestSchemes createLenient()
    {
        List<TestScheme> schemes = new ArrayList<>();

        for (TestSchemeSpec spec : configuration.getSchemes())
        {
            TestScheme scheme = schemeOrNull(spec);

            if (scheme != null)
            {
                schemes.add(scheme);
            }
        }

        return new TestSchemes(schemes.isEmpty() ? List.of(scheme(defaultSpec())) : schemes);
    }

    private TestScheme schemeOrNull(TestSchemeSpec spec)
    {
        try
        {
            return scheme(spec);
        }
        catch (TestPatternException ignored)
        {
            return null;
        }
    }

    private static TestSchemeSpec defaultSpec()
    {
        return new TestSchemeSpec(
            TestSchemeSpec.DEFAULT_NAME,
            PatternBasedTestClassSiblingMediator.DEFAULT_TEST_CLASS_NAME_PATTERN,
            List.of(PatternBasedTestMethodSiblingMediator.DEFAULT_METHOD_NAME_PATTERN));
    }

    private TestScheme scheme(TestSchemeSpec spec)
        throws TestPatternException
    {
        return new TestScheme(
            spec.name,
            new PatternBasedTestClassSiblingMediator(spec.testClass, configuration.isIncludeInterfacesAbstracts()),
            new CompositeTestMethodSiblingMediator(methodMediators(spec)));
    }

    private List<TestMethodSiblingMediator> methodMediators(TestSchemeSpec spec)
    {
        List<TestMethodSiblingMediator> methodMediators = new ArrayList<>();

        for (String testMethodPattern : spec.testMethods)
        {
            methodMediators.add(
                new PatternBasedTestMethodSiblingMediator(
                    testMethodPattern,
                    configuration.getCapitalizationScheme(),
                    configuration.getTestAnnotationFqns(),
                    configuration.isIncludeInheritedMethods(),
                    configuration.isIncludeNestedClasses()));
        }

        return methodMediators;
    }
}
