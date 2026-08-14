package org.bitstrings.idea.plugins.testinsanity.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;

import org.bitstrings.idea.plugins.testinsanity.RenameTestService;
import org.bitstrings.idea.plugins.testinsanity.TestInsanityBundle;
import org.bitstrings.idea.plugins.testinsanity.TestInsanityForm;
import org.bitstrings.idea.plugins.testinsanity.TestSchemesFactory;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternException;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;

public class TestInsanityConfig
    implements SearchableConfigurable, Configurable.NoMargin
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
        form = new TestInsanityForm(project, settings, TestInsanityConfiguration.getInstance(project));

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
        List<TestSchemeSpec> oldSchemes = List.copyOf(settings.getSchemes());

        form.apply();

        try
        {
            validateSchemes();
        }
        catch (ConfigurationException e)
        {
            settings.updateSchemes(oldSchemes);

            throw e;
        }
        finally
        {
            RenameTestService.getInstance(project).update();
        }
    }

    private void validateSchemes()
        throws ConfigurationException
    {
        Set<String> schemeNames = new HashSet<>();

        for (TestSchemeSpec scheme : settings.getSchemes())
        {
            if (!scheme.isComplete())
            {
                throw schemeError(TestInsanityBundle.message("testinsanity.scheme.error.incomplete", scheme.name));
            }

            if (!schemeNames.add(scheme.name))
            {
                throw schemeError(TestInsanityBundle.message("testinsanity.scheme.error.duplicate", scheme.name));
            }
        }

        try
        {
            new TestSchemesFactory(TestInsanityConfiguration.getInstance(project)).create().validatePatterns();
        }
        catch (TestPatternException e)
        {
            throw new ConfigurationException(
                e.getMessage(), e, TestInsanityBundle.message("testinsanity.scheme.error.title"));
        }
    }

    private static ConfigurationException schemeError(String message)
    {
        return new ConfigurationException(message, TestInsanityBundle.message("testinsanity.scheme.error.title"));
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
