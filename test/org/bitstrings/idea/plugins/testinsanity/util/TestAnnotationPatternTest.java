package org.bitstrings.idea.plugins.testinsanity.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings.TestAnnotation;
import org.junit.Test;

public class TestAnnotationPatternTest
{
    @Test
    public void isValid_qualifiedNamesAndPackageWildcards_areAccepted()
    {
        List<String> valid =
            List.of(
                "org.junit.Test", "com.acme.testing.AcmeTest", "com.acme.Outer.Nested", "Test",
                "com.acme.testing.*", "a.*", "com.acme.testing_2.Acme$Inner");

        for (String annotationPattern : valid)
        {
            assertTrue(annotationPattern, TestAnnotationPattern.isValid(annotationPattern));
        }
    }

    @Test
    public void isValid_malformedNamesAndMisplacedWildcards_areRejected()
    {
        List<String> invalid =
            List.of(
                "", "   ", "*", "com.acme.*Test", "com.acme.*.Test", "com.acme.", ".com.acme", "com..acme",
                "@Test", "com.acme.Test ", "com acme.Test", "com.acme.**");

        for (String annotationPattern : invalid)
        {
            assertFalse(annotationPattern, TestAnnotationPattern.isValid(annotationPattern));
        }
    }

    @Test
    public void isValid_null_isRejected()
    {
        assertFalse(TestAnnotationPattern.isValid(null));
    }

    @Test
    public void isValid_everyShippedFrameworkAnnotation_isAccepted()
    {
        for (TestAnnotation testAnnotation : TestAnnotation.values())
        {
            for (String annotationFqn : testAnnotation.getAnnotationsFqns())
            {
                assertTrue(annotationFqn, TestAnnotationPattern.isValid(annotationFqn));
            }
        }
    }
}
