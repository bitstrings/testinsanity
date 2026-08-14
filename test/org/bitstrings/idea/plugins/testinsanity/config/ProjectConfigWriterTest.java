package org.bitstrings.idea.plugins.testinsanity.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings.TestAnnotation;
import org.junit.Test;

public class ProjectConfigWriterTest
{
    @Test
    public void toJson_exportOfADefaultInstall_isAcceptedByTheParser()
    {
        String json = ProjectConfigWriter.toJson(exportOf(new TestInsanitySettings()));

        assertEquals(List.of(), ProjectConfigParser.parse(json).getWarnings());
    }

    @Test
    public void toJson_everyDeclaredKey_survivesTheRoundTrip()
    {
        TestInsanitySettings settings = new TestInsanitySettings();

        settings.updateAdditionalTestAnnotations(List.of("com.acme.AcmeTest", "com.acme.testing.*"));
        settings
            .updateSchemes(
                List.of(
                    new TestSchemeSpec("unit", "${className}Test", List.of("test${subjectName}*")),
                    new TestSchemeSpec("it", "${className}IT", List.of("${subjectName}_+", "${subjectName} *"))));

        ProjectConfig exported = exportOf(settings);

        ProjectConfig reparsed = ProjectConfigParser.parse(ProjectConfigWriter.toJson(exported));

        assertEquals(exported.getSchemes(), reparsed.getSchemes());
        assertEquals(exported.getAdditionalTestAnnotations(), reparsed.getAdditionalTestAnnotations());
        assertEquals(exported.getTestAnnotations(), reparsed.getTestAnnotations());
        assertEquals(exported.getCapitalizeSubject(), reparsed.getCapitalizeSubject());
        assertEquals(exported.getGutterIcons(), reparsed.getGutterIcons());
    }

    @Test
    public void toJson_emptyList_isLeftOutSoTheFileStaysReadable()
    {
        ProjectConfig config = ProjectConfig.absent();

        config.setSchemes(List.of(new TestSchemeSpec("only", "${className}Test", List.of("${subjectName}*"))));
        config.setTestClassPatterns(List.of());
        config.setAdditionalTestAnnotations(List.of());

        String json = ProjectConfigWriter.toJson(config);

        assertFalse(json.contains("testClassPatterns"));
        assertFalse(json.contains("additionalTestAnnotations"));
        assertTrue(json.contains("\"schemes\""));
        assertEquals(List.of(), ProjectConfigParser.parse(json).getWarnings());
    }

    @Test
    public void toJson_anyConfig_endsWithANewline()
    {
        ProjectConfig config = ProjectConfig.absent();

        config.setGutterIcons(true);

        assertTrue(ProjectConfigWriter.toJson(config).endsWith("\n"));
    }

    private static ProjectConfig exportOf(TestInsanitySettings settings)
    {
        ProjectConfig config = ProjectConfig.absent();

        config.setSchemes(settings.resolveSchemes());
        config.setAdditionalTestAnnotations(settings.resolveAdditionalTestAnnotations());
        config.setCapitalizeSubject(settings.getTestMethodNameCapitalizationScheme());
        config.setTestAnnotations(frameworksOf(settings));
        config.setIncludeInheritedMethods(settings.isIncludeInheritedMethods());
        config.setIncludeInterfacesAndAbstracts(settings.isIncludeInterfacesAbstracts());
        config.setIncludeNestedClasses(settings.isIncludeNestedClasses());
        config.setSyncDisplayName(settings.isSyncDisplayName());
        config.setRefactoring(settings.isRefactoringEnabled());
        config.setNavigation(settings.isNavigationEnabled());
        config.setGutterIcons(settings.isGutterAnnotationEnabled());
        config.setPreselectRenames(settings.isPreselectRenames());

        return config;
    }

    private static Set<TestAnnotation> frameworksOf(TestInsanitySettings settings)
    {
        Set<TestAnnotation> frameworks = EnumSet.noneOf(TestAnnotation.class);

        for (TestAnnotation testAnnotation : TestAnnotation.values())
        {
            if (settings.hasTestAnnotation(testAnnotation))
            {
                frameworks.add(testAnnotation);
            }
        }

        return frameworks;
    }
}
