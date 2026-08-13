package org.bitstrings.idea.plugins.testinsanity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.testinsanity.config.ProjectConfigParser;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanityConfiguration;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanityConfiguration.Key;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings.TestAnnotation;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher.CapitalizationScheme;

import com.intellij.ui.IdeBorderFactory;

public final class TestInsanityForm
{
    private static final char PATTERN_SEPARATOR = ';';

    private JPanel settingsPanel;
    private JTextField testClassPatternTextField;
    private JPanel testClassPanel;
    private JPanel testAnnotationsPanel;
    private JCheckBox testAnnotationJunit4CheckBox;
    private JCheckBox testAnnotationJunit5CheckBox;
    private JCheckBox testAnnotationTestNgCheckBox;
    private JPanel testMethodPanel;
    private JTextField testMethodNamePatternTextField;
    private JRadioButton testMethodNameCapSchemeOnlyIfPrefixedRadio;
    private JRadioButton testMethodNameCapSchemeAlwaysRadio;
    private JPanel testMethodNameCapitalizedSubjectPanel;
    private JRadioButton testMethodNameCapSchemeUnchangedRadio;
    private JPanel testMethodPresetPatternsPanel;
    private JEditorPane testMethodPresetPreviewPane;
    private JComboBox<String> testMethodPresetCombo;
    private JButton testMethodPresetSelectButton;
    private JCheckBox enableRefactoringSupportCheckBox;
    private JCheckBox enableNavigationCheckBox;
    private JTextPane recommendedToAvoidFalseTextPane;
    private JCheckBox showGutterAnnotationCheckBox;
    private JCheckBox showRenamingDialogCheckBox;
    private JCheckBox includeInheritedMethodsCheckBox;
    private JCheckBox includeInterfacesAbstractsCheckBox;
    private JCheckBox includeNestedClassesCheckBox;
    private JCheckBox syncDisplayNameCheckBox;

    private final ArrayList<String> presetPreviewPaneTexts = new ArrayList<>();

    private final TestInsanitySettings settings;

    private final TestInsanityConfiguration configuration;

    public TestInsanityForm(TestInsanitySettings settings, TestInsanityConfiguration configuration)
    {
        this.settings = settings;
        this.configuration = configuration;

        init();

        testMethodPresetCombo.removeAllItems();

        for (int index = 0; index < 10; index++)
        {
            String keyPrefix = "testinsanity.preset." + index;
            String patternKey = keyPrefix + ".pattern";

            if (!TestInsanityBundle.containsKey(patternKey))
            {
                break;
            }

            String presetPattern = TestInsanityBundle.message(patternKey);

            if (StringUtils.isEmpty(presetPattern))
            {
                break;
            }

            testMethodPresetCombo.addItem(presetPattern);
            presetPreviewPaneTexts.add(TestInsanityBundle.message(keyPrefix + ".example"));
        }

        if (!presetPreviewPaneTexts.isEmpty())
        {
            testMethodPresetCombo.setSelectedIndex(0);
        }

        showPresetPreview(testMethodPresetCombo.getSelectedIndex());

        testMethodPresetCombo.addActionListener(event -> showPresetPreview(testMethodPresetCombo.getSelectedIndex()));

        testMethodPresetSelectButton.addActionListener(event -> applySelectedPreset());
    }

    private void showPresetPreview(int presetIndex)
    {
        if ((presetIndex >= 0) && (presetIndex < presetPreviewPaneTexts.size()))
        {
            testMethodPresetPreviewPane.setText(presetPreviewPaneTexts.get(presetIndex));
        }
    }

    private void applySelectedPreset()
    {
        Object selectedPreset = testMethodPresetCombo.getSelectedItem();

        if (selectedPreset != null)
        {
            testMethodNamePatternTextField.setText(selectedPreset.toString());
        }
    }

    public JPanel getSettingsPanel()
    {
        testClassPanel.setBorder(IdeBorderFactory.createTitledBorder("Test filename scheme"));
        testAnnotationsPanel.setBorder(IdeBorderFactory.createTitledBorder("Test annotation check"));
        testMethodPanel.setBorder(IdeBorderFactory.createTitledBorder("Test method name scheme"));
        testMethodNameCapitalizedSubjectPanel
            .setBorder(IdeBorderFactory.createTitledBorder("Capitalized subject name"));
        testMethodPresetPatternsPanel
            .setBorder(IdeBorderFactory.createTitledBorder("Preset patterns"));

        return settingsPanel;
    }

    public void init()
    {
        testAnnotationJunit4CheckBox.setSelected(configuration.isTestAnnotationEnabled(TestAnnotation.JUNIT4));
        testAnnotationJunit5CheckBox.setSelected(configuration.isTestAnnotationEnabled(TestAnnotation.JUNIT5));
        testAnnotationTestNgCheckBox.setSelected(configuration.isTestAnnotationEnabled(TestAnnotation.TESTNG));
        testClassPatternTextField.setText(formatPatterns(configuration.getTestClassPatterns()));
        testMethodNamePatternTextField.setText(formatPatterns(configuration.getTestMethodPatterns()));
        testMethodNameCapSchemeOnlyIfPrefixedRadio
            .setSelected(configuration.getCapitalizationScheme() == CapitalizationScheme.IF_PREFIXED);
        testMethodNameCapSchemeAlwaysRadio
            .setSelected(configuration.getCapitalizationScheme() == CapitalizationScheme.ALWAYS);
        testMethodNameCapSchemeUnchangedRadio
            .setSelected(configuration.getCapitalizationScheme() == CapitalizationScheme.UNCHANGED);
        enableRefactoringSupportCheckBox.setSelected(configuration.isRefactoringEnabled());
        enableNavigationCheckBox.setSelected(configuration.isNavigationEnabled());
        showRenamingDialogCheckBox.setSelected(configuration.isPreselectRenames());
        showGutterAnnotationCheckBox.setSelected(configuration.isGutterIconsEnabled());
        includeInheritedMethodsCheckBox.setSelected(configuration.isIncludeInheritedMethods());
        includeInterfacesAbstractsCheckBox.setSelected(configuration.isIncludeInterfacesAbstracts());
        includeNestedClassesCheckBox.setSelected(configuration.isIncludeNestedClasses());
        syncDisplayNameCheckBox.setSelected(configuration.isSyncDisplayName());

        lockGoverned(Key.TEST_CLASS_PATTERNS, testClassPatternTextField);
        lockGoverned(Key.TEST_METHOD_PATTERNS, testMethodNamePatternTextField);
        lockGoverned(
            Key.CAPITALIZE_SUBJECT, testMethodNameCapSchemeOnlyIfPrefixedRadio,
            testMethodNameCapSchemeAlwaysRadio, testMethodNameCapSchemeUnchangedRadio);
        lockGoverned(
            Key.TEST_ANNOTATIONS, testAnnotationJunit4CheckBox, testAnnotationJunit5CheckBox,
            testAnnotationTestNgCheckBox);
        lockGoverned(Key.INCLUDE_INHERITED_METHODS, includeInheritedMethodsCheckBox);
        lockGoverned(Key.INCLUDE_INTERFACES_AND_ABSTRACTS, includeInterfacesAbstractsCheckBox);
        lockGoverned(Key.INCLUDE_NESTED_CLASSES, includeNestedClassesCheckBox);
        lockGoverned(Key.SYNC_DISPLAY_NAME, syncDisplayNameCheckBox);
        lockGoverned(Key.REFACTORING, enableRefactoringSupportCheckBox);
        lockGoverned(Key.NAVIGATION, enableNavigationCheckBox);
        lockGoverned(Key.GUTTER_ICONS, showGutterAnnotationCheckBox);
        lockGoverned(Key.PRESELECT_RENAMES, showRenamingDialogCheckBox);
    }

    private void lockGoverned(Key key, JComponent... components)
    {
        if (isEditable(key))
        {
            return;
        }

        String governedBy =
            TestInsanityBundle.message("testinsanity.config.governed", ProjectConfigParser.FILE_NAME);

        for (JComponent component : components)
        {
            component.setEnabled(false);
            component.setToolTipText(governedBy);
        }
    }

    private boolean isEditable(Key key)
    {
        return !configuration.isGovernedByProjectConfig(key);
    }

    public void apply()
    {
        if (isEditable(Key.TEST_ANNOTATIONS))
        {
            settings.setTestAnnotation(TestAnnotation.JUNIT4, testAnnotationJunit4CheckBox.isSelected());
            settings.setTestAnnotation(TestAnnotation.JUNIT5, testAnnotationJunit5CheckBox.isSelected());
            settings.setTestAnnotation(TestAnnotation.TESTNG, testAnnotationTestNgCheckBox.isSelected());
        }

        if (isEditable(Key.TEST_CLASS_PATTERNS))
        {
            settings.updateTestClassPatterns(parsePatterns(testClassPatternTextField.getText()));
        }

        if (isEditable(Key.TEST_METHOD_PATTERNS))
        {
            settings.updateTestMethodNamePatterns(parsePatterns(testMethodNamePatternTextField.getText()));
        }

        if (isEditable(Key.CAPITALIZE_SUBJECT))
        {
            if (testMethodNameCapSchemeOnlyIfPrefixedRadio.isSelected())
            {
                settings.setTestMethodNameCapitalizationScheme(CapitalizationScheme.IF_PREFIXED);
            }
            else if (testMethodNameCapSchemeAlwaysRadio.isSelected())
            {
                settings.setTestMethodNameCapitalizationScheme(CapitalizationScheme.ALWAYS);
            }
            else if (testMethodNameCapSchemeUnchangedRadio.isSelected())
            {
                settings.setTestMethodNameCapitalizationScheme(CapitalizationScheme.UNCHANGED);
            }
        }

        if (isEditable(Key.REFACTORING))
        {
            settings.setRefactoringEnabled(enableRefactoringSupportCheckBox.isSelected());
        }

        if (isEditable(Key.NAVIGATION))
        {
            settings.setNavigationEnabled(enableNavigationCheckBox.isSelected());
        }

        if (isEditable(Key.PRESELECT_RENAMES))
        {
            settings.setRenamingDialogEnabled(showRenamingDialogCheckBox.isSelected());
        }

        if (isEditable(Key.GUTTER_ICONS))
        {
            settings.setGutterAnnotationEnabled(showGutterAnnotationCheckBox.isSelected());
        }

        if (isEditable(Key.INCLUDE_INHERITED_METHODS))
        {
            settings.setIncludeInheritedMethods(includeInheritedMethodsCheckBox.isSelected());
        }

        if (isEditable(Key.INCLUDE_INTERFACES_AND_ABSTRACTS))
        {
            settings.setIncludeInterfacesAbstracts(includeInterfacesAbstractsCheckBox.isSelected());
        }

        if (isEditable(Key.INCLUDE_NESTED_CLASSES))
        {
            settings.setIncludeNestedClasses(includeNestedClassesCheckBox.isSelected());
        }

        if (isEditable(Key.SYNC_DISPLAY_NAME))
        {
            settings.setSyncDisplayName(syncDisplayNameCheckBox.isSelected());
        }
    }

    public boolean isModified()
    {
        return (isEditable(Key.TEST_CLASS_PATTERNS)
                && !Objects.equals(
                    parsePatterns(testClassPatternTextField.getText()), settings.resolveTestClassPatterns()))
            || (isEditable(Key.TEST_METHOD_PATTERNS)
                && !Objects.equals(
                    parsePatterns(testMethodNamePatternTextField.getText()),
                    settings.resolveTestMethodNamePatterns()))
            || (isEditable(Key.CAPITALIZE_SUBJECT)
                && ((testMethodNameCapSchemeOnlyIfPrefixedRadio.isSelected()
                        != (settings.getTestMethodNameCapitalizationScheme() == CapitalizationScheme.IF_PREFIXED))
                    || (testMethodNameCapSchemeAlwaysRadio.isSelected()
                        != (settings.getTestMethodNameCapitalizationScheme() == CapitalizationScheme.ALWAYS))
                    || (testMethodNameCapSchemeUnchangedRadio.isSelected()
                        != (settings.getTestMethodNameCapitalizationScheme() == CapitalizationScheme.UNCHANGED))))
            || (isEditable(Key.TEST_ANNOTATIONS)
                && ((testAnnotationJunit4CheckBox.isSelected() != settings.hasTestAnnotation(TestAnnotation.JUNIT4))
                    || (testAnnotationJunit5CheckBox.isSelected() != settings.hasTestAnnotation(TestAnnotation.JUNIT5))
                    || (testAnnotationTestNgCheckBox.isSelected() != settings.hasTestAnnotation(TestAnnotation.TESTNG))))
            || (isEditable(Key.REFACTORING)
                && (enableRefactoringSupportCheckBox.isSelected() != settings.isRefactoringEnabled()))
            || (isEditable(Key.NAVIGATION)
                && (enableNavigationCheckBox.isSelected() != settings.isNavigationEnabled()))
            || (isEditable(Key.PRESELECT_RENAMES)
                && (showRenamingDialogCheckBox.isSelected() != settings.isRenamingDialogEnabled()))
            || (isEditable(Key.GUTTER_ICONS)
                && (showGutterAnnotationCheckBox.isSelected() != settings.isGutterAnnotationEnabled()))
            || (isEditable(Key.INCLUDE_INHERITED_METHODS)
                && (includeInheritedMethodsCheckBox.isSelected() != settings.isIncludeInheritedMethods()))
            || (isEditable(Key.INCLUDE_INTERFACES_AND_ABSTRACTS)
                && (includeInterfacesAbstractsCheckBox.isSelected() != settings.isIncludeInterfacesAbstracts()))
            || (isEditable(Key.INCLUDE_NESTED_CLASSES)
                && (includeNestedClassesCheckBox.isSelected() != settings.isIncludeNestedClasses()))
            || (isEditable(Key.SYNC_DISPLAY_NAME)
                && (syncDisplayNameCheckBox.isSelected() != settings.isSyncDisplayName()));
    }

    private static List<String> parsePatterns(String patternsText)
    {
        List<String> patterns = new ArrayList<>();

        for (String pattern : StringUtils.split(StringUtils.defaultString(patternsText), PATTERN_SEPARATOR))
        {
            String trimmedPattern = pattern.trim();

            if (!trimmedPattern.isEmpty())
            {
                patterns.add(trimmedPattern);
            }
        }

        return patterns;
    }

    private static String formatPatterns(List<String> patterns)
    {
        return String.join(String.valueOf(PATTERN_SEPARATOR), patterns);
    }
}
