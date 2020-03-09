package org.bitstrings.idea.plugins.testinsanity.config;

import javax.swing.JComponent;

import org.bitstrings.idea.plugins.testinsanity.PatternBasedTestClassSiblingMediator;
import org.bitstrings.idea.plugins.testinsanity.PatternBasedTestMethodSiblingMediator;
import org.bitstrings.idea.plugins.testinsanity.RenameTestService;
import org.bitstrings.idea.plugins.testinsanity.TestInsanityBundle;
import org.bitstrings.idea.plugins.testinsanity.TestInsanityForm;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternException;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;

public class TestInsanityConfig
    implements SearchableConfigurable
{
    private TestInsanityForm form;
    private TestInsanitySettings settings;

    private final Project project;

    public TestInsanityConfig(Project project)
    {
        this.project = project;
    }

    @Override
    public String getId()
    {
        return TestInsanityConfig.class.getSimpleName();
    }

    @Override
    public String getDisplayName()
    {
        return TestInsanityBundle.message("testinsanity.display.name");
    }

    @Override
    public String getHelpTopic()
    {
        return null;
    }

    @Override
    public JComponent createComponent()
    {
        settings = TestInsanitySettings.getInstance(project);
        form = new TestInsanityForm(settings);

        return form.getSettingsPanel();
    }

    @Override
    public boolean isModified()
    {
        return form.isModified();
    }

    @Override
    public void apply()
        throws ConfigurationException
    {
        String oldTestClassPatterrn = settings.getTestClassPattern();
        String oldTestMethodNamePatterrn = settings.getTestMethodNamePattern();

        form.apply();

        try
        {
            new PatternBasedTestClassSiblingMediator(settings.getTestClassPattern()).validatePattern();
        }
        catch (TestPatternException e)
        {
            settings.setTestClassPattern(oldTestClassPatterrn);
            throw new ConfigurationException(e.getMessage(), e, "Test class pattern error");
        }

        try
        {
            new PatternBasedTestMethodSiblingMediator(
                settings.getTestMethodNamePattern(),
                settings.getTestMethodNameCapitalizationScheme(),
                settings.getTestAnnotations(),
                settings.isIncludeInheritedMethods()
            ).validatePattern();
        }
        catch (TestPatternException e)
        {
            settings.setTestMethodNamePattern(oldTestMethodNamePatterrn);
            throw new ConfigurationException(e.getMessage(), e, "Test method pattern error");
        }

        RenameTestService.getInstance(project).update();
    }

    @Override
    public void reset()
    {
        form.init();
    }

    @Override
    public void disposeUIResources()
    {
    }
}
