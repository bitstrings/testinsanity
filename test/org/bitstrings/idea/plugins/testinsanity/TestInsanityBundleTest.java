package org.bitstrings.idea.plugins.testinsanity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ResourceBundle;

import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher.CapitalizationScheme;
import org.junit.Test;

public class TestInsanityBundleTest
{
    private static final String SUBJECT_TOKEN = "${subjectName}";

    @Test
    public void message_everyPresetPattern_generatesANameThatPatternMatches()
    {
        int presets = 0;

        for (int index = 0; index < 10; index++)
        {
            String patternKey = "testinsanity.preset." + index + ".pattern";

            if (!TestInsanityBundle.containsKey(patternKey))
            {
                continue;
            }

            presets++;

            assertTrue(
                "preset " + index + " has no example",
                TestInsanityBundle.containsKey("testinsanity.preset." + index + ".example"));

            String presetPattern = TestInsanityBundle.message(patternKey);

            TestPatternMatcher matcher =
                new TestPatternMatcher(presetPattern, SUBJECT_TOKEN, true, CapitalizationScheme.IF_PREFIXED);

            String generated = matcher.generateTestName("isDarkColor");

            assertTrue(
                presetPattern + " generated " + generated + " which it does not match itself",
                matcher.findTestMatch(generated, "isDarkColor").isMatched());
        }

        assertTrue("the bundle ships fewer than eight presets", presets >= 8);
    }

    @Test
    public void message_intentionTexts_nameTheCreatedElement()
    {
        assertEquals(
            "Create test class ColorTest",
            TestInsanityBundle.message("testinsanity.intention.create.class", "ColorTest"));
        assertEquals(
            "Create subject method isDark in Color",
            TestInsanityBundle.message("testinsanity.intention.subject.create.method", "isDark", "Color"));
    }

    @Test
    public void message_methodMarkerTexts_separateTheTwoWords()
    {
        assertEquals(
            "Test Method ColorTest.testIsDark (1 Found)",
            TestInsanityBundle.message("testinsanity.marker.method.test", "ColorTest.testIsDark", 1));
        assertEquals(
            "Subject Method Color.isDark (2 Found)",
            TestInsanityBundle.message("testinsanity.marker.method.subject", "Color.isDark", 2));
    }

    @Test
    public void message_navigationHelp_carriesTheResolvedShortcut()
    {
        assertEquals(
            "Navigation also answers the IDE own Navigate | Test (Ctrl+Shift+T).",
            TestInsanityBundle.message("testinsanity.form.feature.help.navigation", "Ctrl+Shift+T"));
    }

    @Test
    public void message_patternHelpText_keepsItsTokensLiteral()
    {
        assertTrue(
            TestInsanityBundle.message("testinsanity.form.schemes.class.help").contains("${className}"));
        assertTrue(
            TestInsanityBundle.message("testinsanity.form.schemes.methods.help").contains(SUBJECT_TOKEN));
    }

    @Test
    public void containsKey_everyKeyTheCodeAsksFor_isDeclared()
    {
        ResourceBundle bundle = ResourceBundle.getBundle("messages.TestInsanityBundle");

        for (String key : REQUIRED_KEYS)
        {
            assertTrue(key, bundle.containsKey(key));
        }
    }

    private static final String[] REQUIRED_KEYS = {
        "testinsanity.settings.name", "testinsanity.display.name", "testinsanity.action.enabler.title",
        "testinsanity.action.jump.test", "testinsanity.action.jump.subject", "testinsanity.action.jump.notfound",
        "testinsanity.renamer.class.option.name", "testinsanity.renamer.class.dialog.title",
        "testinsanity.renamer.class.dialog.description", "testinsanity.renamer.class.dialog.entityname",
        "testinsanity.renamer.method.option.name", "testinsanity.renamer.method.dialog.title",
        "testinsanity.renamer.method.dialog.description", "testinsanity.renamer.method.dialog.entityname",
        "testinsanity.marker.name", "testinsanity.marker.class.tested", "testinsanity.marker.class.missing",
        "testinsanity.marker.class.subject", "testinsanity.marker.class.subject.link",
        "testinsanity.marker.method.missing", "testinsanity.marker.method.test",
        "testinsanity.marker.method.test.link", "testinsanity.marker.method.subject",
        "testinsanity.marker.method.subject.link", "testinsanity.marker.displayname",
        "testinsanity.marker.displayname.link",
        "testinsanity.config.title", "testinsanity.config.unreadable", "testinsanity.config.governed",
        "testinsanity.config.schema.name", "testinsanity.config.section", "testinsanity.config.use",
        "testinsanity.config.status.using", "testinsanity.config.status.ignored",
        "testinsanity.config.status.absent", "testinsanity.config.open", "testinsanity.config.create",
        "testinsanity.config.create.failed",
        "testinsanity.pattern.add.title", "testinsanity.pattern.edit.title", "testinsanity.pattern.method.prompt",
        "testinsanity.scheme.error.title", "testinsanity.scheme.error.incomplete",
        "testinsanity.scheme.error.duplicate",
        "testinsanity.intention.family", "testinsanity.intention.create.class",
        "testinsanity.intention.create.method", "testinsanity.intention.create.class.method",
        "testinsanity.intention.create.choose", "testinsanity.intention.create.choose.title",
        "testinsanity.intention.subject.family", "testinsanity.intention.subject.create.class",
        "testinsanity.intention.subject.create.method",
        "testinsanity.form.schemes.section", "testinsanity.form.schemes.help", "testinsanity.form.schemes.name",
        "testinsanity.form.schemes.class", "testinsanity.form.schemes.class.help",
        "testinsanity.form.schemes.methods", "testinsanity.form.schemes.methods.help",
        "testinsanity.form.schemes.add.title", "testinsanity.form.schemes.add.prompt",
        "testinsanity.form.schemes.name.duplicate",
        "testinsanity.form.class.section", "testinsanity.form.class.interfaces",
        "testinsanity.form.method.section", "testinsanity.form.method.inherited",
        "testinsanity.form.method.nested",
        "testinsanity.form.annotations.section", "testinsanity.form.annotations.help",
        "testinsanity.form.annotations.junit4", "testinsanity.form.annotations.junit5",
        "testinsanity.form.annotations.testng", "testinsanity.form.annotations.additional",
        "testinsanity.form.annotations.additional.help", "testinsanity.form.annotations.additional.prompt",
        "testinsanity.form.annotations.additional.add.title",
        "testinsanity.form.annotations.additional.edit.title",
        "testinsanity.form.annotations.additional.invalid",
        "testinsanity.form.capitalization.section", "testinsanity.form.capitalization.ifprefixed",
        "testinsanity.form.capitalization.always", "testinsanity.form.capitalization.unchanged",
        "testinsanity.form.feature.refactoring", "testinsanity.form.feature.navigation",
        "testinsanity.form.feature.gutter", "testinsanity.form.feature.preselect",
        "testinsanity.form.feature.displayname", "testinsanity.form.feature.help.navigation",
        "testinsanity.form.feature.help.navigation.unbound", "testinsanity.form.feature.help.gutter",
        "testinsanity.form.presets.section", "testinsanity.form.presets.add", "testinsanity.form.presets.help"
    };
}
