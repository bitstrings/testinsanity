package org.bitstrings.idea.plugins.testinsanity.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings.TestAnnotation;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher.CapitalizationScheme;

import com.intellij.openapi.project.Project;

public final class TestInsanityConfiguration
{
    public enum Key
    {
        SCHEMES,
        CAPITALIZE_SUBJECT,
        TEST_ANNOTATIONS,
        ADDITIONAL_TEST_ANNOTATIONS,
        INCLUDE_INHERITED_METHODS,
        INCLUDE_INTERFACES_AND_ABSTRACTS,
        INCLUDE_NESTED_CLASSES,
        SYNC_DISPLAY_NAME,
        REFACTORING,
        NAVIGATION,
        GUTTER_ICONS,
        PRESELECT_RENAMES
    }

    private final TestInsanitySettings settings;

    private final ProjectConfigService configService;

    public TestInsanityConfiguration(Project project)
    {
        this.settings = TestInsanitySettings.getInstance(project);
        this.configService = ProjectConfigService.getInstance(project);
    }

    public static TestInsanityConfiguration getInstance(Project project)
    {
        return project.getService(TestInsanityConfiguration.class);
    }

    public boolean isGovernedByProjectConfig(Key key)
    {
        return settings.isUseProjectConfig() && isDeclaredInProjectConfig(key);
    }

    public boolean isDeclaredInProjectConfig(Key key)
    {
        ProjectConfig config = configService.getConfig();

        switch (key)
        {
            case SCHEMES:
                return (config.getSchemes() != null)
                    || (config.getTestClassPatterns() != null)
                    || (config.getTestMethodPatterns() != null);
            case CAPITALIZE_SUBJECT:
                return (config.getCapitalizeSubject() != null);
            case TEST_ANNOTATIONS:
                return (config.getTestAnnotations() != null);
            case ADDITIONAL_TEST_ANNOTATIONS:
                return (config.getAdditionalTestAnnotations() != null);
            case INCLUDE_INHERITED_METHODS:
                return (config.getIncludeInheritedMethods() != null);
            case INCLUDE_INTERFACES_AND_ABSTRACTS:
                return (config.getIncludeInterfacesAndAbstracts() != null);
            case INCLUDE_NESTED_CLASSES:
                return (config.getIncludeNestedClasses() != null);
            case SYNC_DISPLAY_NAME:
                return (config.getSyncDisplayName() != null);
            case REFACTORING:
                return (config.getRefactoring() != null);
            case NAVIGATION:
                return (config.getNavigation() != null);
            case GUTTER_ICONS:
                return (config.getGutterIcons() != null);
            case PRESELECT_RENAMES:
                return (config.getPreselectRenames() != null);
            default:
                return false;
        }
    }

    public ProjectConfig getProjectConfig()
    {
        return configService.getConfig();
    }

    public ProjectConfigService getProjectConfigService()
    {
        return configService;
    }

    public boolean isProjectConfigPresent()
    {
        return (configService.findConfigFile() != null);
    }

    private ProjectConfig config()
    {
        return settings.isUseProjectConfig() ? configService.getConfig() : ProjectConfig.absent();
    }

    public List<TestSchemeSpec> getSchemes()
    {
        ProjectConfig config = config();

        if (config.getSchemes() != null)
        {
            return config.getSchemes();
        }

        return ((config.getTestClassPatterns() == null) && (config.getTestMethodPatterns() == null))
            ? settings.resolveSchemes()
            : TestSchemeSpec.migrate(getTestClassPatterns(), getTestMethodPatterns());
    }

    private List<String> getTestClassPatterns()
    {
        List<String> patterns = config().getTestClassPatterns();

        return (patterns == null) ? settings.resolveTestClassPatterns() : patterns;
    }

    private List<String> getTestMethodPatterns()
    {
        List<String> patterns = config().getTestMethodPatterns();

        return (patterns == null) ? settings.resolveTestMethodNamePatterns() : patterns;
    }

    public CapitalizationScheme getCapitalizationScheme()
    {
        CapitalizationScheme scheme = config().getCapitalizeSubject();

        return (scheme == null) ? settings.getTestMethodNameCapitalizationScheme() : scheme;
    }

    public Set<String> getTestAnnotationFqns()
    {
        Set<String> annotationFqns = new HashSet<>(frameworkAnnotationFqns());

        annotationFqns.addAll(getAdditionalTestAnnotations());

        return annotationFqns;
    }

    public List<String> getAdditionalTestAnnotations()
    {
        List<String> annotationPatterns = config().getAdditionalTestAnnotations();

        return (annotationPatterns == null)
            ? settings.resolveAdditionalTestAnnotations()
            : annotationPatterns;
    }

    private Set<String> frameworkAnnotationFqns()
    {
        Set<TestAnnotation> testAnnotations = config().getTestAnnotations();

        if (testAnnotations == null)
        {
            return settings.getTestAnnotations();
        }

        Set<String> annotationFqns = new HashSet<>();

        for (TestAnnotation testAnnotation : testAnnotations)
        {
            annotationFqns.addAll(testAnnotation.getAnnotationsFqns());
        }

        return annotationFqns;
    }

    public boolean isTestAnnotationEnabled(TestAnnotation testAnnotation)
    {
        Set<TestAnnotation> testAnnotations = config().getTestAnnotations();

        return (testAnnotations == null)
            ? settings.hasTestAnnotation(testAnnotation)
            : testAnnotations.contains(testAnnotation);
    }

    public boolean isIncludeInheritedMethods()
    {
        return resolve(config().getIncludeInheritedMethods(), settings.isIncludeInheritedMethods());
    }

    public boolean isIncludeInterfacesAbstracts()
    {
        return resolve(
            config().getIncludeInterfacesAndAbstracts(), settings.isIncludeInterfacesAbstracts());
    }

    public boolean isIncludeNestedClasses()
    {
        return resolve(config().getIncludeNestedClasses(), settings.isIncludeNestedClasses());
    }

    public boolean isSyncDisplayName()
    {
        return resolve(config().getSyncDisplayName(), settings.isSyncDisplayName());
    }

    public boolean isRefactoringEnabled()
    {
        return resolve(config().getRefactoring(), settings.isRefactoringEnabled());
    }

    public boolean isNavigationEnabled()
    {
        return resolve(config().getNavigation(), settings.isNavigationEnabled());
    }

    public boolean isGutterIconsEnabled()
    {
        return resolve(config().getGutterIcons(), settings.isGutterAnnotationEnabled());
    }

    public boolean isPreselectRenames()
    {
        return resolve(config().getPreselectRenames(), settings.isPreselectRenames());
    }

    private static boolean resolve(Boolean projectValue, boolean userValue)
    {
        return (projectValue == null) ? userValue : projectValue;
    }
}
