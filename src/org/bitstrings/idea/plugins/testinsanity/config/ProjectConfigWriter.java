package org.bitstrings.idea.plugins.testinsanity.config;

import java.util.List;
import java.util.Set;

import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings.TestAnnotation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class ProjectConfigWriter
{
    private ProjectConfigWriter()
    {
    }

    public static String toJson(ProjectConfig config)
    {
        JsonObject root = new JsonObject();

        addSchemes(root, config.getSchemes());
        addPatterns(root, "testClassPatterns", config.getTestClassPatterns());
        addPatterns(root, "testMethodPatterns", config.getTestMethodPatterns());

        if (config.getCapitalizeSubject() != null)
        {
            root.addProperty("capitalizeSubject", ProjectConfigParser.tokenFor(config.getCapitalizeSubject()));
        }

        addTestAnnotations(root, config.getTestAnnotations());
        addPatterns(root, "additionalTestAnnotations", config.getAdditionalTestAnnotations());

        addFlag(root, "includeInheritedMethods", config.getIncludeInheritedMethods());
        addFlag(root, "includeInterfacesAndAbstracts", config.getIncludeInterfacesAndAbstracts());
        addFlag(root, "includeNestedClasses", config.getIncludeNestedClasses());
        addFlag(root, "syncDisplayName", config.getSyncDisplayName());
        addFlag(root, "refactoring", config.getRefactoring());
        addFlag(root, "navigation", config.getNavigation());
        addFlag(root, "gutterIcons", config.getGutterIcons());
        addFlag(root, "preselectRenames", config.getPreselectRenames());

        return new GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n";
    }

    private static void addSchemes(JsonObject root, List<TestSchemeSpec> schemes)
    {
        if (schemes == null)
        {
            return;
        }

        JsonArray schemeArray = new JsonArray();

        for (TestSchemeSpec scheme : schemes)
        {
            JsonObject schemeObject = new JsonObject();

            schemeObject.addProperty("name", scheme.name);
            schemeObject.addProperty("testClass", scheme.testClass);

            JsonArray testMethods = new JsonArray();

            for (String testMethod : scheme.testMethods)
            {
                testMethods.add(testMethod);
            }

            schemeObject.add("testMethods", testMethods);

            schemeArray.add(schemeObject);
        }

        root.add("schemes", schemeArray);
    }

    private static void addPatterns(JsonObject root, String key, List<String> patterns)
    {
        if ((patterns == null) || patterns.isEmpty())
        {
            return;
        }

        JsonArray patternArray = new JsonArray();

        for (String pattern : patterns)
        {
            patternArray.add(pattern);
        }

        root.add(key, patternArray);
    }

    private static void addTestAnnotations(JsonObject root, Set<TestAnnotation> testAnnotations)
    {
        if (testAnnotations == null)
        {
            return;
        }

        JsonArray annotationArray = new JsonArray();

        for (TestAnnotation testAnnotation : TestAnnotation.values())
        {
            if (testAnnotations.contains(testAnnotation))
            {
                annotationArray.add(ProjectConfigParser.tokenFor(testAnnotation));
            }
        }

        root.add("testAnnotations", annotationArray);
    }

    private static void addFlag(JsonObject root, String key, Boolean value)
    {
        if (value != null)
        {
            root.addProperty(key, value);
        }
    }
}
