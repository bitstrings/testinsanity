package org.bitstrings.idea.plugins.testinsanity;

import java.util.ArrayList;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings.TestAnnotation;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher.CapitalizationScheme;

import com.intellij.ui.IdeBorderFactory;

public class TestInsanityForm
{
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
    private JComboBox testMethodPresetCombo;
    private JButton testMethodPresetSelectButton;
    private JCheckBox enableRefactoringSupportCheckBox;
    private JCheckBox enableNavigationCheckBox;
    private JTextPane itIsRecommendedToTextPane;
    private JCheckBox showGutterAnnotationCheckBox;
    private JCheckBox showRenamingDialogCheckBox;
    private JCheckBox includeInheritedMethodsCheckBox;
    private JCheckBox includeInterfacesAbstractsCheckBox;

    private final ArrayList<String> presetPreviewPaneTexts = new ArrayList<>();

    private final TestInsanitySettings settings;

    public TestInsanityForm(TestInsanitySettings settings)
    {
        this.settings = settings;

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

        testMethodPresetCombo.setSelectedIndex(0);
        testMethodPresetPreviewPane.setText(presetPreviewPaneTexts.get(0));

        testMethodPresetCombo.addActionListener(
            event ->
                testMethodPresetPreviewPane
                    .setText(presetPreviewPaneTexts.get(((JComboBox<?>) event.getSource()).getSelectedIndex()))
        );

        testMethodPresetSelectButton.addActionListener(
            event -> testMethodNamePatternTextField.setText(testMethodPresetCombo.getSelectedItem().toString())
        );
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
        testAnnotationJunit4CheckBox.setSelected(settings.hasTestAnnotation(TestAnnotation.JUNIT4));
        testAnnotationJunit5CheckBox.setSelected(settings.hasTestAnnotation(TestAnnotation.JUNIT5));
        testAnnotationTestNgCheckBox.setSelected(settings.hasTestAnnotation(TestAnnotation.TESTNG));
        testClassPatternTextField.setText(settings.getTestClassPattern());
        testMethodNamePatternTextField.setText(settings.getTestMethodNamePattern());
        testMethodNameCapSchemeOnlyIfPrefixedRadio
            .setSelected(settings.getTestMethodNameCapitalizationScheme() == CapitalizationScheme.IF_PREFIXED);
        testMethodNameCapSchemeAlwaysRadio
            .setSelected(settings.getTestMethodNameCapitalizationScheme() == CapitalizationScheme.ALWAYS);
        testMethodNameCapSchemeUnchangedRadio
            .setSelected(settings.getTestMethodNameCapitalizationScheme() == CapitalizationScheme.UNCHANGED);
        enableRefactoringSupportCheckBox.setSelected(settings.isRefactoringEnabled());
        enableNavigationCheckBox.setSelected(settings.isNavigationEnabled());
        showRenamingDialogCheckBox.setSelected(settings.isRenamingDialogEnabled());
        showGutterAnnotationCheckBox.setSelected(settings.isGutterAnnotationEnabled());
        includeInheritedMethodsCheckBox.setSelected(settings.isIncludeInheritedMethods());
        includeInterfacesAbstractsCheckBox.setSelected(settings.isIncludeInterfacesAbstracts());
    }

    public void apply()
    {
        settings
            .setTestkAnnotation(
                TestAnnotation.JUNIT4, testAnnotationJunit4CheckBox.isSelected()
            );
        settings
            .setTestkAnnotation(
                TestAnnotation.JUNIT5, testAnnotationJunit5CheckBox.isSelected()
            );
        settings
            .setTestkAnnotation(
                TestAnnotation.TESTNG, testAnnotationTestNgCheckBox.isSelected()
            );

        settings.setTestClassPattern(testClassPatternTextField.getText());

        settings.setTestMethodNamePattern(testMethodNamePatternTextField.getText());

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

        settings.setRefactoringEnabled(enableRefactoringSupportCheckBox.isSelected());
        settings.setNavigationEnabled(enableNavigationCheckBox.isSelected());
        settings.setRenamingDialogEnabled(showRenamingDialogCheckBox.isSelected());
        settings.setGutterAnnotationEnabled(showGutterAnnotationCheckBox.isSelected());
        settings.setIncludeInheritedMethods(includeInheritedMethodsCheckBox.isSelected());
        settings.setIncludeInterfacesAbstracts(includeInterfacesAbstractsCheckBox.isSelected());
    }

    public boolean isModified()
    {
        return (!Objects.equals(testClassPatternTextField.getText(), settings.getTestClassPattern()))
            || (!Objects.equals(testMethodNamePatternTextField.getText(), settings.getTestMethodNamePattern()))
            || (testMethodNameCapSchemeOnlyIfPrefixedRadio
                .isSelected() != (settings.getTestMethodNameCapitalizationScheme() == CapitalizationScheme.IF_PREFIXED))
            || (testMethodNameCapSchemeAlwaysRadio
                .isSelected() != (settings.getTestMethodNameCapitalizationScheme() == CapitalizationScheme.ALWAYS))
            || (testMethodNameCapSchemeUnchangedRadio
                .isSelected() != (settings.getTestMethodNameCapitalizationScheme() == CapitalizationScheme.UNCHANGED))
            || (testAnnotationJunit4CheckBox.isSelected() != settings.hasTestAnnotation(TestAnnotation.JUNIT4))
            || (testAnnotationJunit5CheckBox.isSelected() != settings.hasTestAnnotation(TestAnnotation.JUNIT5))
            || (testAnnotationTestNgCheckBox.isSelected() != settings.hasTestAnnotation(TestAnnotation.TESTNG))
            || (enableRefactoringSupportCheckBox.isSelected() != settings.isRefactoringEnabled())
            || (enableNavigationCheckBox.isSelected() != settings.isNavigationEnabled())
            || (showRenamingDialogCheckBox.isSelected() != settings.isNavigationEnabled())
            || (showGutterAnnotationCheckBox.isSelected() != settings.isGutterAnnotationEnabled())
            || (includeInheritedMethodsCheckBox.isSelected() != settings.isIncludeInheritedMethods())
            || (includeInterfacesAbstractsCheckBox.isSelected() != settings.isIncludeInterfacesAbstracts());
    }
}
