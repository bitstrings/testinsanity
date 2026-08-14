package org.bitstrings.idea.plugins.testinsanity.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Set;

import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings.TestAnnotation;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher.CapitalizationScheme;
import org.junit.Test;

public class ProjectConfigParserTest
{
    @Test
    public void parse_declaredSchemes_areReadInOrderWithTheirPatterns()
    {
        ProjectConfig config =
            ProjectConfigParser.parse(
                "{\"schemes\":["
                    + "{\"name\":\"unit\",\"testClass\":\"${className}Test\","
                    + "\"testMethods\":[\"test${subjectName}*\",\"${subjectName}_+\"]},"
                    + "{\"name\":\"it\",\"testClass\":\"${className}IT\","
                    + "\"testMethods\":[\"${subjectName}_+\"]}]}");

        assertEquals(2, config.getSchemes().size());
        assertEquals("unit", config.getSchemes().get(0).name);
        assertEquals(
            List.of("test${subjectName}*", "${subjectName}_+"), config.getSchemes().get(0).testMethods);
        assertEquals("it", config.getSchemes().get(1).name);
        assertEquals(List.of(), config.getWarnings());
    }

    @Test
    public void parse_declaredSchemes_leaveTheLegacyKeysUnset()
    {
        ProjectConfig config =
            ProjectConfigParser.parse(
                "{\"schemes\":[{\"name\":\"unit\",\"testClass\":\"${className}Test\","
                    + "\"testMethods\":[\"${subjectName}*\"]}]}");

        assertNull(config.getTestClassPatterns());
        assertNull(config.getTestMethodPatterns());
    }

    @Test
    public void parse_emptyFile_declaresNothing()
    {
        ProjectConfig config = ProjectConfigParser.parse("{}");

        assertNull(config.getSchemes());
        assertNull(config.getGutterIcons());
        assertNull(config.getAdditionalTestAnnotations());
    }

    @Test
    public void parse_everyFlagAndValue_isRead()
    {
        ProjectConfig config =
            ProjectConfigParser.parse(
                "{\"capitalizeSubject\":\"always\",\"testAnnotations\":[\"junit5\",\"testng\"],"
                    + "\"additionalTestAnnotations\":[\"com.acme.AcmeTest\"],"
                    + "\"includeInheritedMethods\":false,\"includeInterfacesAndAbstracts\":true,"
                    + "\"includeNestedClasses\":false,\"syncDisplayName\":true,\"refactoring\":false,"
                    + "\"navigation\":true,\"gutterIcons\":false,\"preselectRenames\":true}");

        assertEquals(CapitalizationScheme.ALWAYS, config.getCapitalizeSubject());
        assertEquals(Set.of(TestAnnotation.JUNIT5, TestAnnotation.TESTNG), config.getTestAnnotations());
        assertEquals(List.of("com.acme.AcmeTest"), config.getAdditionalTestAnnotations());
        assertEquals(Boolean.FALSE, config.getIncludeInheritedMethods());
        assertEquals(Boolean.TRUE, config.getIncludeInterfacesAndAbstracts());
        assertEquals(Boolean.FALSE, config.getGutterIcons());
        assertEquals(Boolean.TRUE, config.getPreselectRenames());
    }

    @Test
    public void parse_unknownKey_isReportedAsAWarningAndIgnored()
    {
        ProjectConfig config = ProjectConfigParser.parse("{\"gutterIcons\":false,\"bogusKey\":1}");

        assertEquals(Boolean.FALSE, config.getGutterIcons());
        assertEquals(List.of("Unknown setting bogusKey is ignored"), config.getWarnings());
    }

    @Test
    public void parse_malformedSchemes_areRejected()
    {
        assertRejected("{\"schemes\":{}}");
        assertRejected("{\"schemes\":[]}");
        assertRejected("{\"schemes\":[1]}");
        assertRejected(
            "{\"schemes\":[{\"name\":\"a\",\"testClass\":\"${className}T\","
                + "\"testMethods\":[\"${subjectName}*\"],\"x\":1}]}");
        assertRejected("{\"schemes\":[{\"testClass\":\"${className}T\",\"testMethods\":[\"${subjectName}*\"]}]}");
        assertRejected(
            "{\"schemes\":[{\"name\":\"  \",\"testClass\":\"${className}T\","
                + "\"testMethods\":[\"${subjectName}*\"]}]}");
        assertRejected("{\"schemes\":[{\"name\":\"a\",\"testMethods\":[\"${subjectName}*\"]}]}");
        assertRejected("{\"schemes\":[{\"name\":\"a\",\"testClass\":\"${className}T\"}]}");
        assertRejected(
            "{\"schemes\":[{\"name\":\"a\",\"testClass\":\"${className}T\",\"testMethods\":[]}]}");
    }

    @Test
    public void parse_twoSchemesSharingAName_isRejected()
    {
        assertRejected(
            "{\"schemes\":[{\"name\":\"a\",\"testClass\":\"${className}T\",\"testMethods\":[\"${subjectName}*\"]},"
                + "{\"name\":\"a\",\"testClass\":\"${className}IT\",\"testMethods\":[\"${subjectName}*\"]}]}");
    }

    @Test
    public void parse_malformedPatterns_areRejected()
    {
        assertRejected("{\"testClassPatterns\":[\"FooTest\"]}");
        assertRejected("{\"testClassPatterns\":[\"${className}*\"]}");
        assertRejected("{\"testMethodPatterns\":[\"testFoo\"]}");
        assertRejected("{\"testClassPatterns\":[]}");
        assertRejected("{\"testClassPatterns\":[1]}");
        assertRejected("{\"testClassPatterns\":\"${className}Test\"}");
        assertRejected(
            "{\"schemes\":[{\"name\":\"a\",\"testClass\":\"FooTest\",\"testMethods\":[\"${subjectName}*\"]}]}");
        assertRejected(
            "{\"schemes\":[{\"name\":\"a\",\"testClass\":\"${className}T\",\"testMethods\":[\"testFoo\"]}]}");
    }

    @Test
    public void parse_malformedAdditionalAnnotations_areRejected()
    {
        assertRejected("{\"additionalTestAnnotations\":[\"*\"]}");
        assertRejected("{\"additionalTestAnnotations\":[\"com.acme.*Test\"]}");
        assertRejected("{\"additionalTestAnnotations\":[\"com.acme.\"]}");
        assertRejected("{\"additionalTestAnnotations\":[\"@Test\"]}");
        assertRejected("{\"additionalTestAnnotations\":[]}");
    }

    @Test
    public void parse_malformedValues_areRejected()
    {
        assertRejected("{ nope");
        assertRejected("[1,2]");
        assertRejected("{\"capitalizeSubject\":\"sometimes\"}");
        assertRejected("{\"testAnnotations\":[\"junit9\"]}");
        assertRejected("{\"includeNestedClasses\":\"true\"}");
    }

    private static void assertRejected(String json)
    {
        try
        {
            ProjectConfigParser.parse(json);

            fail("The parser accepted " + json);
        }
        catch (ProjectConfigException ignored)
        {
            return;
        }
    }
}
