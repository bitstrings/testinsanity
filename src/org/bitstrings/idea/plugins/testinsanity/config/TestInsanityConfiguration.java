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
        TEST_CLASS_PATTERNS,
        TEST_METHOD_PATTERNS,
        CAPITALIZE_SUBJECT,
        TEST_ANNOTATIONS,
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
        ProjectConfig config = configService.getConfig();

        switch (key)
        {
            case TEST_CLASS_PATTERNS:
                return (config.getTestClassPatterns() != null);
            case TEST_METHOD_PATTERNS:
                return (config.getTestMethodPatterns() != null);
            case CAPITALIZE_SUBJECT:
                return (config.getCapitalizeSubject() != null);
            case TEST_ANNOTATIONS:
                return (config.getTestAnnotations() != null);
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

    public boolean hasProjectConfig()
    {
        return (configService.findConfigFile() != null);
    }

    public List<String> getTestClassPatterns()
    {
        List<String> patterns = configService.getConfig().getTestClassPatterns();

        return (patterns == null) ? settings.resolveTestClassPatterns() : patterns;
    }

    public List<String> getTestMethodPatterns()
    {
        List<String> patterns = configService.getConfig().getTestMethodPatterns();

        return (patterns == null) ? settings.resolveTestMethodNamePatterns() : patterns;
    }

    public CapitalizationScheme getCapitalizationScheme()
    {
        CapitalizationScheme scheme = configService.getConfig().getCapitalizeSubject();

        return (scheme == null) ? settings.getTestMethodNameCapitalizationScheme() : scheme;
    }

    public Set<String> getTestAnnotationFqns()
    {
        Set<TestAnnotation> testAnnotations = configService.getConfig().getTestAnnotations();

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
        Set<TestAnnotation> testAnnotations = configService.getConfig().getTestAnnotations();

        return (testAnnotations == null)
            ? settings.hasTestAnnotation(testAnnotation)
            : testAnnotations.contains(testAnnotation);
    }

    public boolean isIncludeInheritedMethods()
    {
        return resolve(configService.getConfig().getIncludeInheritedMethods(), settings.isIncludeInheritedMethods());
    }

    public boolean isIncludeInterfacesAbstracts()
    {
        return resolve(
            configService.getConfig().getIncludeInterfacesAndAbstracts(), settings.isIncludeInterfacesAbstracts());
    }

    public boolean isIncludeNestedClasses()
    {
        return resolve(configService.getConfig().getIncludeNestedClasses(), settings.isIncludeNestedClasses());
    }

    public boolean isSyncDisplayName()
    {
        return resolve(configService.getConfig().getSyncDisplayName(), settings.isSyncDisplayName());
    }

    public boolean isRefactoringEnabled()
    {
        return resolve(configService.getConfig().getRefactoring(), settings.isRefactoringEnabled());
    }

    public boolean isNavigationEnabled()
    {
        return resolve(configService.getConfig().getNavigation(), settings.isNavigationEnabled());
    }

    public boolean isGutterIconsEnabled()
    {
        return resolve(configService.getConfig().getGutterIcons(), settings.isGutterAnnotationEnabled());
    }

    public boolean isPreselectRenames()
    {
        return resolve(configService.getConfig().getPreselectRenames(), settings.isRenamingDialogEnabled());
    }

    private static boolean resolve(Boolean projectValue, boolean userValue)
    {
        return (projectValue == null) ? userValue : projectValue;
    }
}
