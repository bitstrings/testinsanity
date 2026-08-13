package org.bitstrings.idea.plugins.testinsanity.config;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Set;

import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings.TestAnnotation;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher.CapitalizationScheme;

public final class ProjectConfig
{
    public static final ProjectConfig ABSENT = new ProjectConfig();

    private List<String> testClassPatterns;

    private List<String> testMethodPatterns;

    private CapitalizationScheme capitalizeSubject;

    private Set<TestAnnotation> testAnnotations;

    private Boolean includeInheritedMethods;

    private Boolean includeInterfacesAndAbstracts;

    private Boolean includeNestedClasses;

    private Boolean syncDisplayName;

    private Boolean refactoring;

    private Boolean navigation;

    private Boolean gutterIcons;

    private Boolean preselectRenames;

    private List<String> warnings = emptyList();

    public List<String> getTestClassPatterns()
    {
        return testClassPatterns;
    }

    void setTestClassPatterns(List<String> testClassPatterns)
    {
        this.testClassPatterns = testClassPatterns;
    }

    public List<String> getTestMethodPatterns()
    {
        return testMethodPatterns;
    }

    void setTestMethodPatterns(List<String> testMethodPatterns)
    {
        this.testMethodPatterns = testMethodPatterns;
    }

    public CapitalizationScheme getCapitalizeSubject()
    {
        return capitalizeSubject;
    }

    void setCapitalizeSubject(CapitalizationScheme capitalizeSubject)
    {
        this.capitalizeSubject = capitalizeSubject;
    }

    public Set<TestAnnotation> getTestAnnotations()
    {
        return testAnnotations;
    }

    void setTestAnnotations(Set<TestAnnotation> testAnnotations)
    {
        this.testAnnotations = testAnnotations;
    }

    public Boolean getIncludeInheritedMethods()
    {
        return includeInheritedMethods;
    }

    void setIncludeInheritedMethods(Boolean includeInheritedMethods)
    {
        this.includeInheritedMethods = includeInheritedMethods;
    }

    public Boolean getIncludeInterfacesAndAbstracts()
    {
        return includeInterfacesAndAbstracts;
    }

    void setIncludeInterfacesAndAbstracts(Boolean includeInterfacesAndAbstracts)
    {
        this.includeInterfacesAndAbstracts = includeInterfacesAndAbstracts;
    }

    public Boolean getIncludeNestedClasses()
    {
        return includeNestedClasses;
    }

    void setIncludeNestedClasses(Boolean includeNestedClasses)
    {
        this.includeNestedClasses = includeNestedClasses;
    }

    public Boolean getSyncDisplayName()
    {
        return syncDisplayName;
    }

    void setSyncDisplayName(Boolean syncDisplayName)
    {
        this.syncDisplayName = syncDisplayName;
    }

    public Boolean getRefactoring()
    {
        return refactoring;
    }

    void setRefactoring(Boolean refactoring)
    {
        this.refactoring = refactoring;
    }

    public Boolean getNavigation()
    {
        return navigation;
    }

    void setNavigation(Boolean navigation)
    {
        this.navigation = navigation;
    }

    public Boolean getGutterIcons()
    {
        return gutterIcons;
    }

    void setGutterIcons(Boolean gutterIcons)
    {
        this.gutterIcons = gutterIcons;
    }

    public Boolean getPreselectRenames()
    {
        return preselectRenames;
    }

    void setPreselectRenames(Boolean preselectRenames)
    {
        this.preselectRenames = preselectRenames;
    }

    public List<String> getWarnings()
    {
        return warnings;
    }

    void setWarnings(List<String> warnings)
    {
        this.warnings = List.copyOf(warnings);
    }
}
