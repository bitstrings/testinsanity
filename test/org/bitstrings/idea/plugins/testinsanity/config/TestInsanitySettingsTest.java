package org.bitstrings.idea.plugins.testinsanity.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings.TestAnnotation;
import org.jdom.Element;
import org.junit.Test;

import com.intellij.util.xmlb.SkipDefaultsSerializationFilter;
import com.intellij.util.xmlb.XmlSerializer;

public class TestInsanitySettingsTest
{
    private static final String LEGACY_PRESELECT_OPTION = "renamingDialogEnabled";

    @Test
    public void resolveSchemes_freshSettings_yieldTheDefaultScheme()
    {
        List<TestSchemeSpec> schemes = new TestInsanitySettings().resolveSchemes();

        assertEquals(1, schemes.size());
        assertEquals("${className}Test", schemes.get(0).testClass);
        assertEquals(List.of("(test|)${subjectName}*"), schemes.get(0).testMethods);
    }

    @Test
    public void resolveSchemes_legacyFlatLists_migrateWithEveryMethodPattern()
    {
        TestInsanitySettings settings = new TestInsanitySettings();

        settings.updateTestClassPatterns(List.of("${className}Test", "${className}IT"));
        settings.updateTestMethodNamePatterns(List.of("a${subjectName}*", "b${subjectName}*"));

        List<TestSchemeSpec> schemes = settings.resolveSchemes();

        assertEquals(2, schemes.size());
        assertEquals(List.of("a${subjectName}*", "b${subjectName}*"), schemes.get(1).testMethods);
    }

    @Test
    public void resolveSchemes_legacySinglePattern_isMigrated()
    {
        TestInsanitySettings settings = new TestInsanitySettings();

        settings.testClassPattern = "Test${className}";

        assertEquals("Test${className}", settings.resolveSchemes().get(0).testClass);
    }

    @Test
    public void resolveSchemes_incompleteStoredScheme_fallsBackToTheDefault()
    {
        TestInsanitySettings settings = new TestInsanitySettings();

        settings.getSchemes().add(new TestSchemeSpec(null, null, List.of()));

        assertEquals("${className}Test", settings.resolveSchemes().get(0).testClass);
    }

    @Test
    public void updateSchemes_twoSchemes_mirrorsThePatternsIntoTheLegacyLists()
    {
        TestInsanitySettings settings = new TestInsanitySettings();

        settings
            .updateSchemes(
                List.of(
                    new TestSchemeSpec("unit", "${className}Test", List.of("test${subjectName}*")),
                    new TestSchemeSpec("it", "${className}IT", List.of("${subjectName}_+"))));

        assertEquals(2, settings.resolveSchemes().size());
        assertEquals(List.of("${className}Test", "${className}IT"), settings.resolveTestClassPatterns());
        assertEquals(
            List.of("test${subjectName}*", "${subjectName}_+"), settings.resolveTestMethodNamePatterns());
    }

    @Test
    public void resolveTestClassPatterns_blankStoredPatterns_fallBackToTheDefault()
    {
        TestInsanitySettings settings = new TestInsanitySettings();

        settings.testClassPatterns.add("   ");

        assertEquals(List.of("${className}Test"), settings.resolveTestClassPatterns());
    }

    @Test
    public void loadState_settingsNamingOnlyTheOldJunit5Annotation_stillReportsJunit5Enabled()
    {
        TestInsanitySettings persisted = new TestInsanitySettings();

        persisted.testAnnotations.clear();
        persisted.testAnnotations.add("org.junit.jupiter.api.Test");

        TestInsanitySettings settings = new TestInsanitySettings();

        settings.loadState(persisted);

        assertTrue(settings.hasTestAnnotation(TestAnnotation.JUNIT5));
    }

    @Test
    public void loadState_enabledFramework_isExpandedToTheAnnotationsThisVersionKnows()
    {
        TestInsanitySettings persisted = new TestInsanitySettings();

        persisted.testAnnotations.clear();
        persisted.testAnnotations.add("org.junit.jupiter.api.Test");

        TestInsanitySettings settings = new TestInsanitySettings();

        settings.loadState(persisted);

        assertTrue(
            settings.getTestAnnotations().contains("org.junit.platform.commons.annotation.Testable"));
    }

    @Test
    public void loadState_frameworkTheFileLeftOut_staysDisabled()
    {
        TestInsanitySettings persisted = new TestInsanitySettings();

        persisted.testAnnotations.clear();
        persisted.testAnnotations.add("org.junit.jupiter.api.Test");

        TestInsanitySettings settings = new TestInsanitySettings();

        settings.loadState(persisted);

        assertFalse(settings.hasTestAnnotation(TestAnnotation.TESTNG));
    }

    @Test
    public void loadState_stateWrittenBeforeTheRename_preselectsRenames()
    {
        TestInsanitySettings settings = new TestInsanitySettings();

        settings.loadState(XmlSerializer.deserialize(legacyState(), TestInsanitySettings.class));

        assertTrue(settings.isPreselectRenames());
    }

    @Test
    public void loadState_stateWrittenBeforeTheRename_dropsTheLegacyOption()
    {
        TestInsanitySettings settings = new TestInsanitySettings();

        settings.loadState(XmlSerializer.deserialize(legacyState(), TestInsanitySettings.class));

        assertNull(
            legacyOptionOf(XmlSerializer.serialize(settings.getState(), new SkipDefaultsSerializationFilter())));
    }

    @Test
    public void loadState_preselectRenamesTurnedOff_leavesItOff()
    {
        TestInsanitySettings persisted = new TestInsanitySettings();

        persisted.setPreselectRenames(false);

        TestInsanitySettings settings = new TestInsanitySettings();

        settings.loadState(persisted);

        assertFalse(settings.isPreselectRenames());
    }

    @Test
    public void resolveAdditionalTestAnnotations_blankEntries_areDropped()
    {
        TestInsanitySettings settings = new TestInsanitySettings();

        settings.additionalTestAnnotations.add("   ");
        settings.additionalTestAnnotations.add("com.acme.AcmeTest");

        assertEquals(List.of("com.acme.AcmeTest"), settings.resolveAdditionalTestAnnotations());
    }

    @Test
    public void setTestAnnotation_disablingOneFramework_leavesTheOthersEnabled()
    {
        TestInsanitySettings settings = new TestInsanitySettings();

        settings.setTestAnnotation(TestAnnotation.JUNIT5, false);

        assertFalse(settings.hasTestAnnotation(TestAnnotation.JUNIT5));
        assertTrue(settings.hasTestAnnotation(TestAnnotation.JUNIT4));
        assertTrue(settings.hasTestAnnotation(TestAnnotation.TESTNG));
    }

    private static Element legacyState()
    {
        return new Element("TestInsanitySettings")
            .addContent(
                new Element("option")
                    .setAttribute("name", LEGACY_PRESELECT_OPTION)
                    .setAttribute("value", "false"));
    }

    private static Element legacyOptionOf(Element state)
    {
        for (Element option : state.getChildren("option"))
        {
            if (LEGACY_PRESELECT_OPTION.equals(option.getAttributeValue("name")))
            {
                return option;
            }
        }

        return null;
    }
}
