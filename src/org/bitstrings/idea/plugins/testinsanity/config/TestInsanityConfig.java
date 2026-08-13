package org.bitstrings.idea.plugins.testinsanity.config;

import java.util.List;

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
        form = new TestInsanityForm(settings, TestInsanityConfiguration.getInstance(project));

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
        List<String> oldTestClassPatterns = settings.resolveTestClassPatterns();
        List<String> oldTestMethodNamePatterns = settings.resolveTestMethodNamePatterns();

        form.apply();

        try
        {
            validatePatterns();
        }
        catch (ConfigurationException e)
        {
            settings.updateTestClassPatterns(oldTestClassPatterns);
            settings.updateTestMethodNamePatterns(oldTestMethodNamePatterns);

            throw e;
        }
        finally
        {
            RenameTestService.getInstance(project).update();
        }
    }

    private void validatePatterns()
        throws ConfigurationException
    {
        try
        {
            for (String testClassPattern : settings.resolveTestClassPatterns())
            {
                new PatternBasedTestClassSiblingMediator(
                    testClassPattern, settings.isIncludeInterfacesAbstracts()
                ).validatePattern();
            }
        }
        catch (TestPatternException e)
        {
            throw new ConfigurationException(e.getMessage(), e, "Test class pattern error");
        }

        try
        {
            for (String testMethodNamePattern : settings.resolveTestMethodNamePatterns())
            {
                new PatternBasedTestMethodSiblingMediator(
                    testMethodNamePattern,
                    settings.getTestMethodNameCapitalizationScheme(),
                    settings.getTestAnnotations(),
                    settings.isIncludeInheritedMethods(),
                    settings.isIncludeNestedClasses()
                ).validatePattern();
            }
        }
        catch (TestPatternException e)
        {
            throw new ConfigurationException(e.getMessage(), e, "Test method pattern error");
        }
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
