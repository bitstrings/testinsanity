package org.bitstrings.idea.plugins.testinsanity.config;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Set;

import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings.TestAnnotation;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher.CapitalizationScheme;

public final class ProjectConfig
{
    private List<TestSchemeSpec> schemes;

    private List<String> testClassPatterns;

    private List<String> testMethodPatterns;

    private CapitalizationScheme capitalizeSubject;

    private Set<TestAnnotation> testAnnotations;

    private List<String> additionalTestAnnotations;

    private Boolean includeInheritedMethods;

    private Boolean includeInterfacesAndAbstracts;

    private Boolean includeNestedClasses;

    private Boolean syncDisplayName;

    private Boolean refactoring;

    private Boolean navigation;

    private Boolean gutterIcons;

    private Boolean preselectRenames;

    private List<String> warnings = emptyList();

    public static ProjectConfig absent()
    {
        return new ProjectConfig();
    }

    public List<TestSchemeSpec> getSchemes()
    {
        return schemes;
    }

    public void setSchemes(List<TestSchemeSpec> schemes)
    {
        this.schemes = schemes;
    }

    public List<String> getTestClassPatterns()
    {
        return testClassPatterns;
    }

    public void setTestClassPatterns(List<String> testClassPatterns)
    {
        this.testClassPatterns = testClassPatterns;
    }

    public List<String> getTestMethodPatterns()
    {
        return testMethodPatterns;
    }

    public void setTestMethodPatterns(List<String> testMethodPatterns)
    {
        this.testMethodPatterns = testMethodPatterns;
    }

    public CapitalizationScheme getCapitalizeSubject()
    {
        return capitalizeSubject;
    }

    public void setCapitalizeSubject(CapitalizationScheme capitalizeSubject)
    {
        this.capitalizeSubject = capitalizeSubject;
    }

    public Set<TestAnnotation> getTestAnnotations()
    {
        return testAnnotations;
    }

    public void setTestAnnotations(Set<TestAnnotation> testAnnotations)
    {
        this.testAnnotations = testAnnotations;
    }

    public List<String> getAdditionalTestAnnotations()
    {
        return additionalTestAnnotations;
    }

    public void setAdditionalTestAnnotations(List<String> additionalTestAnnotations)
    {
        this.additionalTestAnnotations = additionalTestAnnotations;
    }

    public Boolean getIncludeInheritedMethods()
    {
        return includeInheritedMethods;
    }

    public void setIncludeInheritedMethods(Boolean includeInheritedMethods)
    {
        this.includeInheritedMethods = includeInheritedMethods;
    }

    public Boolean getIncludeInterfacesAndAbstracts()
    {
        return includeInterfacesAndAbstracts;
    }

    public void setIncludeInterfacesAndAbstracts(Boolean includeInterfacesAndAbstracts)
    {
        this.includeInterfacesAndAbstracts = includeInterfacesAndAbstracts;
    }

    public Boolean getIncludeNestedClasses()
    {
        return includeNestedClasses;
    }

    public void setIncludeNestedClasses(Boolean includeNestedClasses)
    {
        this.includeNestedClasses = includeNestedClasses;
    }

    public Boolean getSyncDisplayName()
    {
        return syncDisplayName;
    }

    public void setSyncDisplayName(Boolean syncDisplayName)
    {
        this.syncDisplayName = syncDisplayName;
    }

    public Boolean getRefactoring()
    {
        return refactoring;
    }

    public void setRefactoring(Boolean refactoring)
    {
        this.refactoring = refactoring;
    }

    public Boolean getNavigation()
    {
        return navigation;
    }

    public void setNavigation(Boolean navigation)
    {
        this.navigation = navigation;
    }

    public Boolean getGutterIcons()
    {
        return gutterIcons;
    }

    public void setGutterIcons(Boolean gutterIcons)
    {
        this.gutterIcons = gutterIcons;
    }

    public Boolean getPreselectRenames()
    {
        return preselectRenames;
    }

    public void setPreselectRenames(Boolean preselectRenames)
    {
        this.preselectRenames = preselectRenames;
    }

    public List<String> getWarnings()
    {
        return warnings;
    }

    public void setWarnings(List<String> warnings)
    {
        this.warnings = List.copyOf(warnings);
    }
}
