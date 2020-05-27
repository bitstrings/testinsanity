package org.bitstrings.idea.plugins.testinsanity.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.bitstrings.idea.plugins.testinsanity.PatternBasedTestClassSiblingMediator;
import org.bitstrings.idea.plugins.testinsanity.PatternBasedTestMethodSiblingMediator;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher.CapitalizationScheme;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "TestInsanitySettings", storages = @Storage("TestInsanity.xml"))
public class TestInsanitySettings
    implements PersistentStateComponent<TestInsanitySettings>
{
    public enum TestAnnotation
    {
        JUNIT4("org.junit.Test"),
        JUNIT5(
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.TestFactory",
            "org.junit.jupiter.params.ParameterizedTest",
            "org.junit.jupiter.api.RepeatedTest"
        ),
        TESTNG("org.testng.annotations.Test");

        private Set<String> annotationsFqns;

        TestAnnotation(String... annotationsFqns)
        {
            this.annotationsFqns = Set.of(annotationsFqns);
        }

        public Set<String> getAnnotationsFqns()
        {
            return annotationsFqns;
        }
    }

    public final Set<String> testAnnotations = new HashSet<>();

    public String testClassPattern;

    public String testMethodNamePattern;

    public CapitalizationScheme testMethodNameCapitalizationScheme;

    public boolean refactoringEnabled;

    public boolean navigationEnabled;

    public boolean renamingDialogEnabled;

    public boolean gutterAnnotationEnabled;

    public boolean includeInheritedMethods;

    public boolean includeInterfacesAbstracts;

    public TestInsanitySettings()
    {
        setTestkAnnotation(TestAnnotation.JUNIT4, true);
        setTestkAnnotation(TestAnnotation.JUNIT5, true);
        setTestkAnnotation(TestAnnotation.TESTNG, true);
        setTestClassPattern(PatternBasedTestClassSiblingMediator.DEFAULT_TEST_CLASS_NAME_PATTERN);
        setTestMethodNamePattern(PatternBasedTestMethodSiblingMediator.DEFAULT_METHOD_NAME_PATTERN);
        setTestMethodNameCapitalizationScheme(CapitalizationScheme.IF_PREFIXED);
        setRefactoringEnabled(true);
        setNavigationEnabled(true);
        setRenamingDialogEnabled(true);
        setGutterAnnotationEnabled(true);
        setIncludeInheritedMethods(true);
        setIncludeInterfacesAbstracts(false);
    }

    public static TestInsanitySettings getInstance(Project project)
    {
        return project == null
            ? ServiceManager.getService(TestInsanitySettings.class)
            : ServiceManager.getService(project, TestInsanitySettings.class);
    }

    @Override
    public void initializeComponent()
    {
    }

    @Override
    public TestInsanitySettings getState()
    {
        return this;
    }

    @Override
    public void loadState(TestInsanitySettings settings)
    {
        XmlSerializerUtil.copyBean(settings, this);
    }

    public void setTestkAnnotation(TestAnnotation annotation, boolean enabled)
    {
        if (enabled)
        {
            testAnnotations.addAll(annotation.getAnnotationsFqns());
        }
        else
        {
            testAnnotations.removeAll(annotation.getAnnotationsFqns());
        }
    }

    public boolean hasTestAnnotation(TestAnnotation annotation)
    {
        return !Collections.disjoint(testAnnotations, annotation.getAnnotationsFqns());
    }

    public Set<String> getTestAnnotations()
    {
        return testAnnotations;
    }

    public void setTestClassPattern(String testClassPattern)
    {
        this.testClassPattern = testClassPattern;
    }

    public String getTestClassPattern()
    {
        return testClassPattern;
    }

    public void setTestMethodNamePattern(String testMethodNamePattern)
    {
        this.testMethodNamePattern = testMethodNamePattern;
    }

    public String getTestMethodNamePattern()
    {
        return testMethodNamePattern;
    }

    public void setTestMethodNameCapitalizationScheme(
        CapitalizationScheme testMethodNameCapitalizationScheme)
    {
        this.testMethodNameCapitalizationScheme = testMethodNameCapitalizationScheme;
    }

    public CapitalizationScheme getTestMethodNameCapitalizationScheme()
    {
        return testMethodNameCapitalizationScheme;
    }

    public void setRefactoringEnabled(boolean refactoringEnabled)
    {
        this.refactoringEnabled = refactoringEnabled;
    }

    public boolean isRefactoringEnabled()
    {
        return refactoringEnabled;
    }

    public void setNavigationEnabled(boolean navigationEnabled)
    {
        this.navigationEnabled = navigationEnabled;
    }

    public boolean isNavigationEnabled()
    {
        return navigationEnabled;
    }

    public void setRenamingDialogEnabled(boolean renamingDialogEnabled)
    {
        this.renamingDialogEnabled = renamingDialogEnabled;
    }

    public boolean isRenamingDialogEnabled()
    {
        return renamingDialogEnabled;
    }

    public void setGutterAnnotationEnabled(boolean gutterAnnotationEnabled)
    {
        this.gutterAnnotationEnabled = gutterAnnotationEnabled;
    }

    public boolean isGutterAnnotationEnabled()
    {
        return gutterAnnotationEnabled;
    }

    public void setIncludeInheritedMethods(boolean includeInheritedMethods)
    {
        this.includeInheritedMethods = includeInheritedMethods;
    }

    public boolean isIncludeInheritedMethods()
    {
        return includeInheritedMethods;
    }

    public void setIncludeInterfacesAbstracts(boolean includeInterfacesAbstracts)
    {
        this.includeInterfacesAbstracts = includeInterfacesAbstracts;
    }

    public boolean isIncludeInterfacesAbstracts()
    {
        return includeInterfacesAbstracts;
    }
}
