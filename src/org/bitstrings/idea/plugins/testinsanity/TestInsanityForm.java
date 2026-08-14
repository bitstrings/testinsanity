package org.bitstrings.idea.plugins.testinsanity;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.testinsanity.config.ProjectConfig;
import org.bitstrings.idea.plugins.testinsanity.config.ProjectConfigParser;
import org.bitstrings.idea.plugins.testinsanity.config.ProjectConfigService;
import org.bitstrings.idea.plugins.testinsanity.config.ProjectConfigWriter;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanityConfiguration;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanityConfiguration.Key;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings.TestAnnotation;
import org.bitstrings.idea.plugins.testinsanity.config.TestSchemeSpec;
import org.bitstrings.idea.plugins.testinsanity.util.TestAnnotationPattern;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternException;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher.CapitalizationScheme;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

public final class TestInsanityForm
{
    private static final class WrapLayout
        extends FlowLayout
    {
        private static final long serialVersionUID = 1L;

        WrapLayout()
        {
            super(FlowLayout.LEFT, JBUI.scale(12), JBUI.scale(4));
        }

        @Override
        public Dimension preferredLayoutSize(Container target)
        {
            return layoutSize(target);
        }

        @Override
        public Dimension minimumLayoutSize(Container target)
        {
            return layoutSize(target);
        }

        private int widestMember(Container target)
        {
            int widest = 0;

            for (Component member : target.getComponents())
            {
                if (member.isVisible())
                {
                    widest = Math.max(widest, member.getPreferredSize().width);
                }
            }

            return widest;
        }

        private Dimension layoutSize(Container target)
        {
            synchronized (target.getTreeLock())
            {
                int widestMember = widestMember(target);

                int maximumWidth = (target.getWidth() > 0) ? target.getWidth() : widestMember;

                int rowWidth = 0;
                int rowHeight = 0;
                int totalHeight = 0;

                for (Component member : target.getComponents())
                {
                    if (!member.isVisible())
                    {
                        continue;
                    }

                    Dimension memberSize = member.getPreferredSize();

                    if ((rowWidth > 0) && ((rowWidth + getHgap() + memberSize.width) > maximumWidth))
                    {
                        totalHeight += rowHeight + getVgap();
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    rowWidth += ((rowWidth > 0) ? getHgap() : 0) + memberSize.width;
                    rowHeight = Math.max(rowHeight, memberSize.height);
                }

                Insets insets = target.getInsets();

                return new Dimension(
                    widestMember + insets.left + insets.right,
                    totalHeight + rowHeight + insets.top + insets.bottom);
            }
        }
    }

    private static final class SettingsPanel
        extends JPanel
        implements Scrollable
    {
        private static final long serialVersionUID = 1L;

        SettingsPanel(JComponent content)
        {
            super(new BorderLayout());

            add(content, BorderLayout.CENTER);

            setBorder(
                JBUI.Borders.empty(PAGE_MARGIN_TOP, PAGE_MARGIN_SIDE, PAGE_MARGIN_BOTTOM, PAGE_MARGIN_SIDE));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize()
        {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
        {
            return JBUI.scale(SCROLL_UNIT);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
        {
            return (orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth()
        {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight()
        {
            return (getParent() instanceof JViewport) && (getParent().getHeight() > getPreferredSize().height);
        }
    }

    private static final class SchemeCellRenderer
        extends ColoredListCellRenderer<TestSchemeSpec>
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected void customizeCellRenderer(
            JList<? extends TestSchemeSpec> list, TestSchemeSpec scheme, int index, boolean selected, boolean hasFocus
        )
        {
            append(StringUtils.defaultString(scheme.name));
            append(" " + StringUtils.defaultString(scheme.testClass), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }

    private static final int MAX_PRESETS = 10;

    private static final String GOTO_TEST_ACTION_ID = "GotoTest";

    private static final int FIELD_COLUMNS = 16;

    private static final int PAGE_MARGIN_TOP = 11;

    private static final int PAGE_MARGIN_SIDE = 16;

    private static final int PAGE_MARGIN_BOTTOM = 16;

    private static final int SCROLL_UNIT = 16;

    private final JBCheckBox useProjectConfigCheckBox =
        new JBCheckBox(TestInsanityBundle.message("testinsanity.config.use", ProjectConfigParser.FILE_NAME));

    private final JBTextArea projectConfigStatusLabel = helpText("");

    private final HyperlinkLabel projectConfigLink = new HyperlinkLabel();

    private final CollectionListModel<TestSchemeSpec> schemesModel = new CollectionListModel<>();

    private final JBList<TestSchemeSpec> schemesList = new JBList<>(schemesModel);

    private final JBTextField schemeNameField = new JBTextField(FIELD_COLUMNS);

    private final JBTextField testClassPatternField = new JBTextField(FIELD_COLUMNS);

    private final JBTextArea schemeMethodsLabel = wrappingText("");

    private final CollectionListModel<String> testMethodPatternsModel = new CollectionListModel<>();

    private final JBList<String> testMethodPatternsList = new JBList<>(testMethodPatternsModel);

    private final JBCheckBox includeInterfacesAbstractsCheckBox =
        new JBCheckBox(TestInsanityBundle.message("testinsanity.form.class.interfaces"));

    private final JBCheckBox testAnnotationJunit4CheckBox =
        new JBCheckBox(TestInsanityBundle.message("testinsanity.form.annotations.junit4"));

    private final JBCheckBox testAnnotationJunit5CheckBox =
        new JBCheckBox(TestInsanityBundle.message("testinsanity.form.annotations.junit5"));

    private final JBCheckBox testAnnotationTestNgCheckBox =
        new JBCheckBox(TestInsanityBundle.message("testinsanity.form.annotations.testng"));

    private final CollectionListModel<String> additionalAnnotationsModel = new CollectionListModel<>();

    private final JBList<String> additionalAnnotationsList = new JBList<>(additionalAnnotationsModel);

    private final JBRadioButton capitalizeIfPrefixedRadio =
        new JBRadioButton(TestInsanityBundle.message("testinsanity.form.capitalization.ifprefixed"));

    private final JBRadioButton capitalizeAlwaysRadio =
        new JBRadioButton(TestInsanityBundle.message("testinsanity.form.capitalization.always"));

    private final JBRadioButton capitalizeUnchangedRadio =
        new JBRadioButton(TestInsanityBundle.message("testinsanity.form.capitalization.unchanged"));

    private final JBCheckBox includeInheritedMethodsCheckBox =
        new JBCheckBox(TestInsanityBundle.message("testinsanity.form.method.inherited"));

    private final JBCheckBox includeNestedClassesCheckBox =
        new JBCheckBox(TestInsanityBundle.message("testinsanity.form.method.nested"));

    private final JBCheckBox refactoringCheckBox =
        new JBCheckBox(TestInsanityBundle.message("testinsanity.form.feature.refactoring"));

    private final JBCheckBox navigationCheckBox =
        new JBCheckBox(TestInsanityBundle.message("testinsanity.form.feature.navigation"));

    private final JBCheckBox gutterIconsCheckBox =
        new JBCheckBox(TestInsanityBundle.message("testinsanity.form.feature.gutter"));

    private final JBCheckBox preselectRenamesCheckBox =
        new JBCheckBox(TestInsanityBundle.message("testinsanity.form.feature.preselect"));

    private final JBCheckBox syncDisplayNameCheckBox =
        new JBCheckBox(TestInsanityBundle.message("testinsanity.form.feature.displayname"));

    private final ComboBox<String> presetCombo = new ComboBox<>();

    private final JButton presetAddButton =
        new JButton(TestInsanityBundle.message("testinsanity.form.presets.add"));

    private final JEditorPane presetPreviewPane = new JEditorPane();

    private final List<String> presetPatterns = new ArrayList<>();

    private final List<String> presetPreviews = new ArrayList<>();

    private final Project project;

    private final TestInsanitySettings settings;

    private final TestInsanityConfiguration configuration;

    private final JPanel schemesPanel;

    private final JPanel additionalAnnotationsPanel;

    private final JComponent settingsPanel;

    private int editedIndex = -1;

    private boolean loadingScheme;

    public TestInsanityForm(Project project, TestInsanitySettings settings, TestInsanityConfiguration configuration)
    {
        this.project = project;
        this.settings = settings;
        this.configuration = configuration;

        this.schemesPanel = createSchemesPanel();
        this.additionalAnnotationsPanel = createAdditionalAnnotationsPanel();

        loadPresets();

        this.settingsPanel = createSettingsPanel();

        if (!presetPatterns.isEmpty())
        {
            presetCombo.setSelectedIndex(0);
        }

        showPresetPreview(presetCombo.getSelectedIndex());

        init();

        useProjectConfigCheckBox.addItemListener(event -> init());
        projectConfigLink.addHyperlinkListener(event -> openOrCreateProjectConfig());
    }

    public JComponent getSettingsPanel()
    {
        return settingsPanel;
    }

    public void init()
    {
        useProjectConfigCheckBox.setSelected(settings.isUseProjectConfig());

        ProjectConfig projectConfig = configuration.getProjectConfig();

        testAnnotationJunit4CheckBox.setSelected(displayedTestAnnotation(TestAnnotation.JUNIT4));
        testAnnotationJunit5CheckBox.setSelected(displayedTestAnnotation(TestAnnotation.JUNIT5));
        testAnnotationTestNgCheckBox.setSelected(displayedTestAnnotation(TestAnnotation.TESTNG));

        loadSchemes(governs(Key.SCHEMES) ? configuration.getSchemes() : settings.resolveSchemes());

        additionalAnnotationsModel.replaceAll(
            governs(Key.ADDITIONAL_TEST_ANNOTATIONS)
                ? new ArrayList<>(projectConfig.getAdditionalTestAnnotations())
                : new ArrayList<>(settings.resolveAdditionalTestAnnotations()));

        CapitalizationScheme scheme =
            governs(Key.CAPITALIZE_SUBJECT)
                ? projectConfig.getCapitalizeSubject()
                : settings.getTestMethodNameCapitalizationScheme();

        capitalizeIfPrefixedRadio.setSelected(scheme == CapitalizationScheme.IF_PREFIXED);
        capitalizeAlwaysRadio.setSelected(scheme == CapitalizationScheme.ALWAYS);
        capitalizeUnchangedRadio.setSelected(scheme == CapitalizationScheme.UNCHANGED);

        refactoringCheckBox.setSelected(
            displayedFlag(Key.REFACTORING, projectConfig.getRefactoring(), settings.isRefactoringEnabled()));
        navigationCheckBox.setSelected(
            displayedFlag(Key.NAVIGATION, projectConfig.getNavigation(), settings.isNavigationEnabled()));
        preselectRenamesCheckBox.setSelected(
            displayedFlag(
                Key.PRESELECT_RENAMES, projectConfig.getPreselectRenames(), settings.isPreselectRenames()));
        gutterIconsCheckBox.setSelected(
            displayedFlag(Key.GUTTER_ICONS, projectConfig.getGutterIcons(), settings.isGutterAnnotationEnabled()));
        includeInheritedMethodsCheckBox.setSelected(
            displayedFlag(
                Key.INCLUDE_INHERITED_METHODS, projectConfig.getIncludeInheritedMethods(),
                settings.isIncludeInheritedMethods()));
        includeInterfacesAbstractsCheckBox.setSelected(
            displayedFlag(
                Key.INCLUDE_INTERFACES_AND_ABSTRACTS, projectConfig.getIncludeInterfacesAndAbstracts(),
                settings.isIncludeInterfacesAbstracts()));
        includeNestedClassesCheckBox.setSelected(
            displayedFlag(
                Key.INCLUDE_NESTED_CLASSES, projectConfig.getIncludeNestedClasses(),
                settings.isIncludeNestedClasses()));
        syncDisplayNameCheckBox.setSelected(
            displayedFlag(Key.SYNC_DISPLAY_NAME, projectConfig.getSyncDisplayName(), settings.isSyncDisplayName()));

        applyGovernedLocks();
        updateProjectConfigStatus();
    }

    public void apply()
    {
        settings.setUseProjectConfig(useProjectConfigCheckBox.isSelected());

        if (isEditable(Key.TEST_ANNOTATIONS))
        {
            settings.setTestAnnotation(TestAnnotation.JUNIT4, testAnnotationJunit4CheckBox.isSelected());
            settings.setTestAnnotation(TestAnnotation.JUNIT5, testAnnotationJunit5CheckBox.isSelected());
            settings.setTestAnnotation(TestAnnotation.TESTNG, testAnnotationTestNgCheckBox.isSelected());
        }

        if (isEditable(Key.ADDITIONAL_TEST_ANNOTATIONS))
        {
            settings.updateAdditionalTestAnnotations(additionalAnnotationsModel.getItems());
        }

        if (isEditable(Key.SCHEMES))
        {
            settings.updateSchemes(editedSchemes());
        }

        if (isEditable(Key.CAPITALIZE_SUBJECT))
        {
            settings.setTestMethodNameCapitalizationScheme(selectedCapitalizationScheme());
        }

        if (isEditable(Key.REFACTORING))
        {
            settings.setRefactoringEnabled(refactoringCheckBox.isSelected());
        }

        if (isEditable(Key.NAVIGATION))
        {
            settings.setNavigationEnabled(navigationCheckBox.isSelected());
        }

        if (isEditable(Key.PRESELECT_RENAMES))
        {
            settings.setPreselectRenames(preselectRenamesCheckBox.isSelected());
        }

        if (isEditable(Key.GUTTER_ICONS))
        {
            settings.setGutterAnnotationEnabled(gutterIconsCheckBox.isSelected());
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
        return (useProjectConfigCheckBox.isSelected() != settings.isUseProjectConfig())
            || (isEditable(Key.SCHEMES) && !editedSchemes().equals(settings.resolveSchemes()))
            || (isEditable(Key.CAPITALIZE_SUBJECT)
                && (selectedCapitalizationScheme() != settings.getTestMethodNameCapitalizationScheme()))
            || (isEditable(Key.TEST_ANNOTATIONS)
                && ((testAnnotationJunit4CheckBox.isSelected() != settings.hasTestAnnotation(TestAnnotation.JUNIT4))
                    || (testAnnotationJunit5CheckBox.isSelected() != settings.hasTestAnnotation(TestAnnotation.JUNIT5))
                    || (testAnnotationTestNgCheckBox.isSelected()
                        != settings.hasTestAnnotation(TestAnnotation.TESTNG))))
            || (isEditable(Key.ADDITIONAL_TEST_ANNOTATIONS)
                && !additionalAnnotationsModel
                    .getItems()
                    .equals(settings.resolveAdditionalTestAnnotations()))
            || (isEditable(Key.REFACTORING)
                && (refactoringCheckBox.isSelected() != settings.isRefactoringEnabled()))
            || (isEditable(Key.NAVIGATION)
                && (navigationCheckBox.isSelected() != settings.isNavigationEnabled()))
            || (isEditable(Key.PRESELECT_RENAMES)
                && (preselectRenamesCheckBox.isSelected() != settings.isPreselectRenames()))
            || (isEditable(Key.GUTTER_ICONS)
                && (gutterIconsCheckBox.isSelected() != settings.isGutterAnnotationEnabled()))
            || (isEditable(Key.INCLUDE_INHERITED_METHODS)
                && (includeInheritedMethodsCheckBox.isSelected() != settings.isIncludeInheritedMethods()))
            || (isEditable(Key.INCLUDE_INTERFACES_AND_ABSTRACTS)
                && (includeInterfacesAbstractsCheckBox.isSelected() != settings.isIncludeInterfacesAbstracts()))
            || (isEditable(Key.INCLUDE_NESTED_CLASSES)
                && (includeNestedClassesCheckBox.isSelected() != settings.isIncludeNestedClasses()))
            || (isEditable(Key.SYNC_DISPLAY_NAME)
                && (syncDisplayNameCheckBox.isSelected() != settings.isSyncDisplayName()));
    }

    private boolean displayedTestAnnotation(TestAnnotation testAnnotation)
    {
        return governs(Key.TEST_ANNOTATIONS)
            ? configuration.getProjectConfig().getTestAnnotations().contains(testAnnotation)
            : settings.hasTestAnnotation(testAnnotation);
    }

    private boolean displayedFlag(Key key, Boolean projectValue, boolean userValue)
    {
        return governs(key) ? projectValue : userValue;
    }

    private JComponent createSettingsPanel()
    {
        return new SettingsPanel(createSettingsContent());
    }

    private JComponent createSettingsContent()
    {
        return FormBuilder
            .createFormBuilder()
            .addComponent(createProjectConfigPanel())
            .addComponent(
                row(
                    refactoringCheckBox, preselectRenamesCheckBox, gutterIconsCheckBox, navigationCheckBox,
                    syncDisplayNameCheckBox))
            .addComponent(helpText(navigationHelp()))
            .addComponent(helpText(TestInsanityBundle.message("testinsanity.form.feature.help.gutter")))
            .addComponent(schemesPanel)
            .addComponent(createTestClassPanel())
            .addComponent(createTestAnnotationsPanel())
            .addComponent(createTestMethodPanel())
            .addComponentFillVertically(createPresetPanel(), 0)
            .getPanel();
    }

    private static String navigationHelp()
    {
        AnAction gotoTestAction = ActionManager.getInstance().getAction(GOTO_TEST_ACTION_ID);

        String shortcut = (gotoTestAction == null) ? "" : KeymapUtil.getFirstKeyboardShortcutText(gotoTestAction);

        return StringUtils.isBlank(shortcut)
            ? TestInsanityBundle.message("testinsanity.form.feature.help.navigation.unbound")
            : TestInsanityBundle.message("testinsanity.form.feature.help.navigation", shortcut);
    }

    private JPanel createProjectConfigPanel()
    {
        JPanel panel =
            FormBuilder
                .createFormBuilder()
                .addComponent(useProjectConfigCheckBox)
                .addComponent(projectConfigStatusLabel)
                .addComponent(projectConfigLink)
                .getPanel();

        panel.setBorder(
            IdeBorderFactory.createTitledBorder(TestInsanityBundle.message("testinsanity.config.section")));

        return panel;
    }

    private JPanel createSchemesPanel()
    {
        JPanel panel =
            FormBuilder
                .createFormBuilder()
                .addComponent(createSchemesListPanel())
                .addComponent(helpText(TestInsanityBundle.message("testinsanity.form.schemes.help")))
                .addLabeledComponent(
                    TestInsanityBundle.message("testinsanity.form.schemes.name"), schemeNameField)
                .addLabeledComponent(
                    TestInsanityBundle.message("testinsanity.form.schemes.class"), testClassPatternField)
                .addComponent(helpText(TestInsanityBundle.message("testinsanity.form.schemes.class.help")))
                .addComponent(schemeMethodsLabel)
                .addComponent(createTestMethodPatternsPanel())
                .addComponent(helpText(TestInsanityBundle.message("testinsanity.form.schemes.methods.help")))
                .getPanel();

        panel.setBorder(
            IdeBorderFactory.createTitledBorder(TestInsanityBundle.message("testinsanity.form.schemes.section")));

        return panel;
    }

    private JPanel createSchemesListPanel()
    {
        schemesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        schemesList.setVisibleRowCount(4);
        schemesList.setCellRenderer(new SchemeCellRenderer());
        schemesList.addListSelectionListener(
            event ->
            {
                if (!event.getValueIsAdjusting())
                {
                    selectScheme(schemesList.getSelectedIndex());
                }
            });

        storeSchemeOnChange(schemeNameField);
        storeSchemeOnChange(testClassPatternField);

        JPanel panel =
            ToolbarDecorator
                .createDecorator(schemesList)
                .setAddAction(button -> addScheme())
                .setRemoveAction(button -> removeSelectedScheme())
                .setRemoveActionUpdater(event -> schemesModel.getSize() > 1)
                .createPanel();

        panel.setPreferredSize(new Dimension(0, JBUI.scale(110)));

        return panel;
    }

    private void storeSchemeOnChange(JBTextField field)
    {
        field.getDocument().addDocumentListener(
            new DocumentAdapter()
            {
                @Override
                protected void textChanged(DocumentEvent event)
                {
                    storeEditedScheme();

                    updateSchemeMethodsLabel();

                    schemesList.repaint();
                }
            });
    }

    private JPanel createTestMethodPatternsPanel()
    {
        testMethodPatternsList.setVisibleRowCount(4);

        testMethodPatternsModel.addListDataListener(
            new ListDataListener()
            {
                @Override
                public void intervalAdded(ListDataEvent event)
                {
                    storeEditedScheme();
                }

                @Override
                public void intervalRemoved(ListDataEvent event)
                {
                    storeEditedScheme();
                }

                @Override
                public void contentsChanged(ListDataEvent event)
                {
                    storeEditedScheme();
                }
            });

        JPanel panel =
            ToolbarDecorator
                .createDecorator(testMethodPatternsList)
                .setAddAction(button -> editPattern(null))
                .setEditAction(button -> editSelectedPattern())
                .setRemoveActionUpdater(event -> testMethodPatternsModel.getSize() > 1)
                .createPanel();

        panel.setPreferredSize(new Dimension(0, JBUI.scale(110)));

        return panel;
    }

    private JPanel createAdditionalAnnotationsPanel()
    {
        additionalAnnotationsList.setVisibleRowCount(3);

        JPanel panel =
            ToolbarDecorator
                .createDecorator(additionalAnnotationsList)
                .setAddAction(button -> editAdditionalAnnotation(null))
                .setEditAction(button -> editSelectedAdditionalAnnotation())
                .createPanel();

        panel.setPreferredSize(new Dimension(0, JBUI.scale(90)));

        return panel;
    }

    private void editSelectedAdditionalAnnotation()
    {
        int selectedIndex = additionalAnnotationsList.getSelectedIndex();

        if (selectedIndex >= 0)
        {
            editAdditionalAnnotation(additionalAnnotationsModel.getElementAt(selectedIndex));
        }
    }

    private void editAdditionalAnnotation(String currentAnnotation)
    {
        String annotationPattern =
            Messages.showInputDialog(
                project,
                TestInsanityBundle.message("testinsanity.form.annotations.additional.prompt"),
                TestInsanityBundle.message(
                    (currentAnnotation == null)
                        ? "testinsanity.form.annotations.additional.add.title"
                        : "testinsanity.form.annotations.additional.edit.title"),
                null,
                currentAnnotation,
                annotationValidator());

        if (StringUtils.isBlank(annotationPattern))
        {
            return;
        }

        if (currentAnnotation == null)
        {
            additionalAnnotationsModel.add(annotationPattern.trim());
        }
        else
        {
            additionalAnnotationsModel.setElementAt(
                annotationPattern.trim(), additionalAnnotationsModel.getElementIndex(currentAnnotation));
        }
    }

    private static InputValidatorEx annotationValidator()
    {
        return new InputValidatorEx()
        {
            @Override
            public String getErrorText(String inputString)
            {
                if (StringUtils.isBlank(inputString) || TestAnnotationPattern.isValid(inputString.trim()))
                {
                    return null;
                }

                return TestInsanityBundle.message(
                    "testinsanity.form.annotations.additional.invalid", TestAnnotationPattern.PACKAGE_WILDCARD_SUFFIX);
            }

            @Override
            public boolean checkInput(String inputString)
            {
                return !StringUtils.isBlank(inputString) && (getErrorText(inputString) == null);
            }

            @Override
            public boolean canClose(String inputString)
            {
                return checkInput(inputString);
            }
        };
    }

    private void loadSchemes(List<TestSchemeSpec> schemes)
    {
        List<TestSchemeSpec> editableSchemes = new ArrayList<>();

        for (TestSchemeSpec scheme : schemes)
        {
            editableSchemes.add(scheme.copy());
        }

        editedIndex = -1;

        schemesModel.replaceAll(editableSchemes);

        int selectedIndex = editableSchemes.isEmpty() ? -1 : 0;

        schemesList.setSelectedIndex(selectedIndex);

        selectScheme(selectedIndex);
    }

    private void selectScheme(int schemeIndex)
    {
        TestSchemeSpec scheme =
            ((schemeIndex < 0) || (schemeIndex >= schemesModel.getSize()))
                ? new TestSchemeSpec("", "", List.of())
                : schemesModel.getElementAt(schemeIndex).copy();

        loadingScheme = true;

        try
        {
            editedIndex = schemeIndex;

            schemeNameField.setText(scheme.name);
            testClassPatternField.setText(scheme.testClass);
            testMethodPatternsModel.replaceAll(scheme.testMethods);

            updateSchemeMethodsLabel();
        }
        finally
        {
            loadingScheme = false;
        }
    }

    private void updateSchemeMethodsLabel()
    {
        String schemeName = schemeNameField.getText().trim();

        schemeMethodsLabel.setText(
            StringUtils.isBlank(schemeName)
                ? TestInsanityBundle.message("testinsanity.form.schemes.methods.none")
                : TestInsanityBundle.message("testinsanity.form.schemes.methods", schemeName));
    }

    private void storeEditedScheme()
    {
        if (loadingScheme || (editedIndex < 0) || (editedIndex >= schemesModel.getSize()))
        {
            return;
        }

        TestSchemeSpec scheme = schemesModel.getElementAt(editedIndex);

        scheme.name = schemeNameField.getText().trim();
        scheme.testClass = testClassPatternField.getText().trim();
        scheme.testMethods.clear();
        scheme.testMethods.addAll(testMethodPatternsModel.getItems());
    }

    private void addScheme()
    {
        String name =
            Messages.showInputDialog(
                project,
                TestInsanityBundle.message("testinsanity.form.schemes.add.prompt"),
                TestInsanityBundle.message("testinsanity.form.schemes.add.title"),
                null,
                null,
                schemeNameValidator());

        if (StringUtils.isBlank(name))
        {
            return;
        }

        editedIndex = -1;

        schemesModel.add(
            new TestSchemeSpec(
                name.trim(),
                PatternBasedTestClassSiblingMediator.DEFAULT_TEST_CLASS_NAME_PATTERN,
                List.of(PatternBasedTestMethodSiblingMediator.DEFAULT_METHOD_NAME_PATTERN)));

        schemesList.setSelectedIndex(schemesModel.getSize() - 1);
    }

    private void removeSelectedScheme()
    {
        int selectedIndex = schemesList.getSelectedIndex();

        if ((selectedIndex < 0) || (schemesModel.getSize() <= 1))
        {
            return;
        }

        editedIndex = -1;

        schemesModel.remove(selectedIndex);

        schemesList.setSelectedIndex(Math.min(selectedIndex, schemesModel.getSize() - 1));
    }

    private InputValidatorEx schemeNameValidator()
    {
        return new InputValidatorEx()
        {
            @Override
            public String getErrorText(String inputString)
            {
                if (StringUtils.isBlank(inputString))
                {
                    return null;
                }

                return isSchemeNameTaken(inputString.trim())
                    ? TestInsanityBundle.message("testinsanity.form.schemes.name.duplicate", inputString.trim())
                    : null;
            }

            @Override
            public boolean checkInput(String inputString)
            {
                return !StringUtils.isBlank(inputString) && (getErrorText(inputString) == null);
            }

            @Override
            public boolean canClose(String inputString)
            {
                return checkInput(inputString);
            }
        };
    }

    private boolean isSchemeNameTaken(String name)
    {
        for (TestSchemeSpec scheme : schemesModel.getItems())
        {
            if (name.equals(scheme.name))
            {
                return true;
            }
        }

        return false;
    }

    private List<TestSchemeSpec> editedSchemes()
    {
        List<TestSchemeSpec> edited = new ArrayList<>();

        for (TestSchemeSpec scheme : schemesModel.getItems())
        {
            edited.add(scheme.copy());
        }

        return edited;
    }

    private JPanel createTestClassPanel()
    {
        JPanel panel =
            FormBuilder
                .createFormBuilder()
                .addComponent(includeInterfacesAbstractsCheckBox)
                .getPanel();

        panel.setBorder(
            IdeBorderFactory.createTitledBorder(TestInsanityBundle.message("testinsanity.form.class.section")));

        return panel;
    }

    private JPanel createTestAnnotationsPanel()
    {
        JPanel panel =
            FormBuilder
                .createFormBuilder()
                .addComponent(
                    row(testAnnotationJunit4CheckBox, testAnnotationJunit5CheckBox, testAnnotationTestNgCheckBox))
                .addComponent(helpText(TestInsanityBundle.message("testinsanity.form.annotations.help")))
                .addComponent(
                    new JBLabel(TestInsanityBundle.message("testinsanity.form.annotations.additional")))
                .addComponent(additionalAnnotationsPanel)
                .addComponent(helpText(TestInsanityBundle.message("testinsanity.form.annotations.additional.help")))
                .getPanel();

        panel.setBorder(
            IdeBorderFactory.createTitledBorder(TestInsanityBundle.message("testinsanity.form.annotations.section")));

        return panel;
    }

    private JPanel createTestMethodPanel()
    {
        JPanel panel =
            FormBuilder
                .createFormBuilder()
                .addComponent(row(includeInheritedMethodsCheckBox, includeNestedClassesCheckBox))
                .addComponent(createCapitalizationPanel())
                .getPanel();

        panel.setBorder(
            IdeBorderFactory.createTitledBorder(TestInsanityBundle.message("testinsanity.form.method.section")));

        return panel;
    }

    private void editSelectedPattern()
    {
        int selectedIndex = testMethodPatternsList.getSelectedIndex();

        if (selectedIndex >= 0)
        {
            editPattern(testMethodPatternsModel.getElementAt(selectedIndex));
        }
    }

    private void editPattern(String currentPattern)
    {
        String pattern =
            Messages.showInputDialog(
                project,
                TestInsanityBundle.message("testinsanity.pattern.method.prompt"),
                TestInsanityBundle.message(
                    (currentPattern == null) ? "testinsanity.pattern.add.title" : "testinsanity.pattern.edit.title"),
                null,
                currentPattern,
                patternValidator());

        if (StringUtils.isBlank(pattern))
        {
            return;
        }

        if (currentPattern == null)
        {
            testMethodPatternsModel.add(pattern.trim());
        }
        else
        {
            testMethodPatternsModel.setElementAt(
                pattern.trim(), testMethodPatternsModel.getElementIndex(currentPattern));
        }
    }

    private static InputValidatorEx patternValidator()
    {
        return new InputValidatorEx()
        {
            @Override
            public String getErrorText(String inputString)
            {
                return StringUtils.isBlank(inputString) ? null : validateMethodPattern(inputString.trim());
            }

            @Override
            public boolean checkInput(String inputString)
            {
                return !StringUtils.isBlank(inputString) && (getErrorText(inputString) == null);
            }

            @Override
            public boolean canClose(String inputString)
            {
                return checkInput(inputString);
            }
        };
    }

    private static String validateMethodPattern(String pattern)
    {
        try
        {
            new PatternBasedTestMethodSiblingMediator(
                pattern, CapitalizationScheme.IF_PREFIXED, Set.of(), true, true).validatePattern();

            return null;
        }
        catch (TestPatternException e)
        {
            return e.getMessage();
        }
    }

    private JPanel createCapitalizationPanel()
    {
        ButtonGroup capitalizationGroup = new ButtonGroup();

        capitalizationGroup.add(capitalizeIfPrefixedRadio);
        capitalizationGroup.add(capitalizeAlwaysRadio);
        capitalizationGroup.add(capitalizeUnchangedRadio);

        JPanel panel = row(capitalizeIfPrefixedRadio, capitalizeAlwaysRadio, capitalizeUnchangedRadio);

        panel.setBorder(
            IdeBorderFactory
                .createTitledBorder(TestInsanityBundle.message("testinsanity.form.capitalization.section")));

        return panel;
    }

    private JPanel createPresetPanel()
    {
        presetPreviewPane.setContentType("text/html");
        presetPreviewPane.setEditable(false);
        presetPreviewPane.setOpaque(false);

        presetCombo.addActionListener(event -> showPresetPreview(presetCombo.getSelectedIndex()));
        presetAddButton.addActionListener(event -> addSelectedPreset());

        JBScrollPane previewScrollPane = new JBScrollPane(presetPreviewPane);

        previewScrollPane.setPreferredSize(new Dimension(0, JBUI.scale(240)));

        JPanel panel =
            FormBuilder
                .createFormBuilder()
                .addComponent(row(presetCombo, presetAddButton))
                .addComponent(helpText(TestInsanityBundle.message("testinsanity.form.presets.help")))
                .addComponentFillVertically(previewScrollPane, 0)
                .getPanel();

        panel.setBorder(
            IdeBorderFactory.createTitledBorder(TestInsanityBundle.message("testinsanity.form.presets.section")));

        return panel;
    }

    private void loadPresets()
    {
        for (int index = 0; index < MAX_PRESETS; index++)
        {
            String keyPrefix = "testinsanity.preset." + index;

            if (!TestInsanityBundle.containsKey(keyPrefix + ".pattern"))
            {
                break;
            }

            String presetPattern = TestInsanityBundle.message(keyPrefix + ".pattern");

            if (StringUtils.isEmpty(presetPattern))
            {
                break;
            }

            presetPatterns.add(presetPattern);
            presetPreviews.add(TestInsanityBundle.message(keyPrefix + ".example"));
            presetCombo.addItem(presetPattern);
        }
    }

    private void showPresetPreview(int presetIndex)
    {
        if ((presetIndex >= 0) && (presetIndex < presetPreviews.size()))
        {
            presetPreviewPane.setText(presetPreviews.get(presetIndex));
        }
    }

    private void addSelectedPreset()
    {
        int presetIndex = presetCombo.getSelectedIndex();

        if ((presetIndex < 0) || (presetIndex >= presetPatterns.size()) || !isEditable(Key.SCHEMES))
        {
            return;
        }

        String presetPattern = presetPatterns.get(presetIndex);

        if (!testMethodPatternsModel.getItems().contains(presetPattern))
        {
            testMethodPatternsModel.add(presetPattern);
        }
    }

    private CapitalizationScheme selectedCapitalizationScheme()
    {
        if (capitalizeAlwaysRadio.isSelected())
        {
            return CapitalizationScheme.ALWAYS;
        }

        return capitalizeUnchangedRadio.isSelected()
            ? CapitalizationScheme.UNCHANGED
            : CapitalizationScheme.IF_PREFIXED;
    }

    private void applyGovernedLocks()
    {
        setGoverned(Key.SCHEMES, schemesPanel);
        setGoverned(
            Key.CAPITALIZE_SUBJECT, capitalizeIfPrefixedRadio, capitalizeAlwaysRadio, capitalizeUnchangedRadio);
        setGoverned(
            Key.TEST_ANNOTATIONS, testAnnotationJunit4CheckBox, testAnnotationJunit5CheckBox,
            testAnnotationTestNgCheckBox);
        setGoverned(Key.ADDITIONAL_TEST_ANNOTATIONS, additionalAnnotationsPanel);
        setGoverned(Key.INCLUDE_INHERITED_METHODS, includeInheritedMethodsCheckBox);
        setGoverned(Key.INCLUDE_INTERFACES_AND_ABSTRACTS, includeInterfacesAbstractsCheckBox);
        setGoverned(Key.INCLUDE_NESTED_CLASSES, includeNestedClassesCheckBox);
        setGoverned(Key.SYNC_DISPLAY_NAME, syncDisplayNameCheckBox);
        setGoverned(Key.REFACTORING, refactoringCheckBox);
        setGoverned(Key.NAVIGATION, navigationCheckBox);
        setGoverned(Key.GUTTER_ICONS, gutterIconsCheckBox);
        setGoverned(Key.PRESELECT_RENAMES, preselectRenamesCheckBox);

        presetAddButton.setEnabled(isEditable(Key.SCHEMES));
    }

    private void updateProjectConfigStatus()
    {
        String statusKey =
            !configuration.isProjectConfigPresent()
                ? "testinsanity.config.status.absent"
                : (useProjectConfigCheckBox.isSelected()
                    ? "testinsanity.config.status.using"
                    : "testinsanity.config.status.ignored");

        projectConfigStatusLabel.setText(TestInsanityBundle.message(statusKey, ProjectConfigParser.FILE_NAME));

        projectConfigLink.setHyperlinkText(
            TestInsanityBundle.message(
                configuration.isProjectConfigPresent()
                    ? "testinsanity.config.open"
                    : "testinsanity.config.create",
                ProjectConfigParser.FILE_NAME));
    }

    private void openOrCreateProjectConfig()
    {
        ProjectConfigService configService = configuration.getProjectConfigService();

        VirtualFile configFile = configService.findConfigFile();

        if (configFile == null)
        {
            try
            {
                configFile = configService.createConfigFile(ProjectConfigWriter.toJson(asProjectConfig()));
            }
            catch (IOException e)
            {
                Messages.showErrorDialog(
                    project,
                    TestInsanityBundle.message(
                        "testinsanity.config.create.failed", ProjectConfigParser.FILE_NAME, e.getMessage()),
                    TestInsanityBundle.message("testinsanity.config.title"));

                return;
            }
        }

        if (configFile != null)
        {
            FileEditorManager.getInstance(project).openFile(configFile, true);
        }

        init();
    }

    private ProjectConfig asProjectConfig()
    {
        ProjectConfig config = new ProjectConfig();

        List<TestSchemeSpec> schemes = editedSchemes();

        config.setSchemes(schemes.isEmpty() ? settings.resolveSchemes() : schemes);
        config.setCapitalizeSubject(selectedCapitalizationScheme());
        config.setTestAnnotations(selectedTestAnnotations());
        config.setAdditionalTestAnnotations(additionalAnnotationsModel.getItems());
        config.setIncludeInheritedMethods(includeInheritedMethodsCheckBox.isSelected());
        config.setIncludeInterfacesAndAbstracts(includeInterfacesAbstractsCheckBox.isSelected());
        config.setIncludeNestedClasses(includeNestedClassesCheckBox.isSelected());
        config.setSyncDisplayName(syncDisplayNameCheckBox.isSelected());
        config.setRefactoring(refactoringCheckBox.isSelected());
        config.setNavigation(navigationCheckBox.isSelected());
        config.setGutterIcons(gutterIconsCheckBox.isSelected());
        config.setPreselectRenames(preselectRenamesCheckBox.isSelected());

        return config;
    }

    private Set<TestAnnotation> selectedTestAnnotations()
    {
        Set<TestAnnotation> testAnnotations = EnumSet.noneOf(TestAnnotation.class);

        if (testAnnotationJunit4CheckBox.isSelected())
        {
            testAnnotations.add(TestAnnotation.JUNIT4);
        }

        if (testAnnotationJunit5CheckBox.isSelected())
        {
            testAnnotations.add(TestAnnotation.JUNIT5);
        }

        if (testAnnotationTestNgCheckBox.isSelected())
        {
            testAnnotations.add(TestAnnotation.TESTNG);
        }

        return testAnnotations;
    }

    private void setGoverned(Key key, JComponent... components)
    {
        boolean editable = isEditable(key);

        String governedBy =
            editable
                ? null
                : TestInsanityBundle.message("testinsanity.config.governed", ProjectConfigParser.FILE_NAME);

        for (JComponent component : components)
        {
            UIUtil.setEnabled(component, editable, true);

            component.setToolTipText(governedBy);
        }
    }

    private boolean governs(Key key)
    {
        return useProjectConfigCheckBox.isSelected() && configuration.isDeclaredInProjectConfig(key);
    }

    private boolean isEditable(Key key)
    {
        return !governs(key);
    }

    private static JPanel row(JComponent... components)
    {
        JPanel panel = new JPanel(new WrapLayout());

        for (JComponent component : components)
        {
            panel.add(component);
        }

        return panel;
    }

    private static JBTextArea helpText(String text)
    {
        JBTextArea helpText = wrappingText(text);

        helpText.setForeground(UIUtil.getContextHelpForeground());
        helpText.setBorder(JBUI.Borders.emptyBottom(4));

        return helpText;
    }

    private static JBTextArea wrappingText(String text)
    {
        JBTextArea wrappingText = new JBTextArea(text);

        wrappingText.setColumns(1);
        wrappingText.setLineWrap(true);
        wrappingText.setWrapStyleWord(true);
        wrappingText.setEditable(false);
        wrappingText.setFocusable(false);
        wrappingText.setOpaque(false);
        wrappingText.setFont(UIUtil.getLabelFont());

        return wrappingText;
    }
}
