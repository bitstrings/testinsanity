package org.bitstrings.idea.plugins.testinsanity.config;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.bitstrings.idea.plugins.testinsanity.PatternBasedTestClassSiblingMediator;
import org.bitstrings.idea.plugins.testinsanity.PatternBasedTestMethodSiblingMediator;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings.TestAnnotation;
import org.bitstrings.idea.plugins.testinsanity.util.TestAnnotationPattern;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternException;
import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher.CapitalizationScheme;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public final class ProjectConfigParser
{
    public static final String FILE_NAME = ".testinsanity.json";

    private static final String SCHEMA_KEY = "$schema";

    private static final Map<String, CapitalizationScheme> CAPITALIZATION_SCHEMES =
        Map.of(
            "unchanged", CapitalizationScheme.UNCHANGED,
            "ifPrefixed", CapitalizationScheme.IF_PREFIXED,
            "always", CapitalizationScheme.ALWAYS);

    private static final Map<String, TestAnnotation> TEST_ANNOTATIONS =
        Map.of(
            "junit4", TestAnnotation.JUNIT4,
            "junit5", TestAnnotation.JUNIT5,
            "testng", TestAnnotation.TESTNG);

    private static final Set<String> SCHEME_KEYS = Set.of("name", "testClass", "testMethods");

    private static final Set<String> KNOWN_KEYS =
        Set.of(
            SCHEMA_KEY, "schemes", "testClassPatterns", "testMethodPatterns", "capitalizeSubject", "testAnnotations",
            "additionalTestAnnotations", "includeInheritedMethods", "includeInterfacesAndAbstracts",
            "includeNestedClasses", "syncDisplayName", "refactoring", "navigation", "gutterIcons",
            "preselectRenames");

    private ProjectConfigParser()
    {
    }

    public static String tokenFor(CapitalizationScheme scheme)
    {
        return keyFor(CAPITALIZATION_SCHEMES, scheme);
    }

    public static String tokenFor(TestAnnotation testAnnotation)
    {
        return keyFor(TEST_ANNOTATIONS, testAnnotation);
    }

    private static <T> String keyFor(Map<String, T> tokens, T value)
    {
        for (Map.Entry<String, T> token : tokens.entrySet())
        {
            if (token.getValue() == value)
            {
                return token.getKey();
            }
        }

        throw new IllegalArgumentException("No configuration token for " + value);
    }

    public static ProjectConfig parse(String json)
        throws ProjectConfigException
    {
        JsonObject root = readRoot(json);

        List<String> warnings = new ArrayList<>();

        for (String key : root.keySet())
        {
            if (!KNOWN_KEYS.contains(key))
            {
                warnings.add("Unknown setting " + key + " is ignored");
            }
        }

        ProjectConfig config = new ProjectConfig();

        config.setSchemes(readSchemes(root));
        config.setTestClassPatterns(validateClassPatterns(readPatterns(root, "testClassPatterns")));
        config.setTestMethodPatterns(validateMethodPatterns(readPatterns(root, "testMethodPatterns")));
        config.setCapitalizeSubject(readEnum(root, "capitalizeSubject", CAPITALIZATION_SCHEMES));
        config.setTestAnnotations(readTestAnnotations(root));
        config.setAdditionalTestAnnotations(
            validateAnnotationPatterns(readPatterns(root, "additionalTestAnnotations")));
        config.setIncludeInheritedMethods(readBoolean(root, "includeInheritedMethods"));
        config.setIncludeInterfacesAndAbstracts(readBoolean(root, "includeInterfacesAndAbstracts"));
        config.setIncludeNestedClasses(readBoolean(root, "includeNestedClasses"));
        config.setSyncDisplayName(readBoolean(root, "syncDisplayName"));
        config.setRefactoring(readBoolean(root, "refactoring"));
        config.setNavigation(readBoolean(root, "navigation"));
        config.setGutterIcons(readBoolean(root, "gutterIcons"));
        config.setPreselectRenames(readBoolean(root, "preselectRenames"));
        config.setWarnings(warnings);

        return config;
    }

    private static List<TestSchemeSpec> readSchemes(JsonObject root)
    {
        JsonElement value = root.get("schemes");

        if ((value == null) || value.isJsonNull())
        {
            return null;
        }

        if (!value.isJsonArray())
        {
            throw new ProjectConfigException("schemes must be an array of scheme objects");
        }

        List<TestSchemeSpec> schemes = new ArrayList<>();

        for (JsonElement element : value.getAsJsonArray())
        {
            if (!element.isJsonObject())
            {
                throw new ProjectConfigException(
                    "schemes must contain only objects declaring name, testClass and testMethods");
            }

            schemes.add(readScheme(element.getAsJsonObject(), schemes));
        }

        if (schemes.isEmpty())
        {
            throw new ProjectConfigException("schemes must declare at least one scheme");
        }

        return schemes;
    }

    private static TestSchemeSpec readScheme(JsonObject scheme, List<TestSchemeSpec> declaredSchemes)
    {
        for (String key : scheme.keySet())
        {
            if (!SCHEME_KEYS.contains(key))
            {
                throw new ProjectConfigException(
                    "Unknown scheme setting " + key + ", expected one of "
                        + String.join(", ", new TreeSet<>(SCHEME_KEYS)));
            }
        }

        String name = readString(scheme, "name");

        if (name == null)
        {
            throw new ProjectConfigException("Every scheme needs a name");
        }

        for (TestSchemeSpec declaredScheme : declaredSchemes)
        {
            if (name.equals(declaredScheme.name))
            {
                throw new ProjectConfigException("Duplicate scheme name " + name);
            }
        }

        String testClass = readString(scheme, "testClass");

        if (testClass == null)
        {
            throw new ProjectConfigException("Scheme " + name + " needs a testClass pattern");
        }

        List<String> testMethods = readPatterns(scheme, "testMethods");

        if (testMethods == null)
        {
            throw new ProjectConfigException("Scheme " + name + " needs at least one testMethods pattern");
        }

        validateClassPatterns(List.of(testClass));
        validateMethodPatterns(testMethods);

        return new TestSchemeSpec(name, testClass, testMethods);
    }

    private static String readString(JsonObject root, String key)
    {
        JsonElement value = root.get(key);

        if ((value == null) || value.isJsonNull())
        {
            return null;
        }

        if (!isString(value))
        {
            throw new ProjectConfigException(key + " must be a string");
        }

        String text = value.getAsString().trim();

        return text.isEmpty() ? null : text;
    }

    private static List<String> validateClassPatterns(List<String> patterns)
    {
        if (patterns != null)
        {
            for (String pattern : patterns)
            {
                try
                {
                    new PatternBasedTestClassSiblingMediator(pattern, false).validatePattern();
                }
                catch (TestPatternException e)
                {
                    throw new ProjectConfigException("testClassPatterns " + pattern + ": " + e.getMessage(), e);
                }
            }
        }

        return patterns;
    }

    private static List<String> validateAnnotationPatterns(List<String> annotationPatterns)
    {
        if (annotationPatterns != null)
        {
            for (String annotationPattern : annotationPatterns)
            {
                if (!TestAnnotationPattern.isValid(annotationPattern))
                {
                    throw new ProjectConfigException(
                        "additionalTestAnnotations " + annotationPattern
                            + " is not a fully qualified annotation name or a "
                            + TestAnnotationPattern.PACKAGE_WILDCARD_SUFFIX + " package wildcard");
                }
            }
        }

        return annotationPatterns;
    }

    private static List<String> validateMethodPatterns(List<String> patterns)
    {
        if (patterns != null)
        {
            for (String pattern : patterns)
            {
                try
                {
                    new PatternBasedTestMethodSiblingMediator(
                        pattern, CapitalizationScheme.IF_PREFIXED, Set.of(), true, true).validatePattern();
                }
                catch (TestPatternException e)
                {
                    throw new ProjectConfigException("testMethodPatterns " + pattern + ": " + e.getMessage(), e);
                }
            }
        }

        return patterns;
    }

    private static JsonObject readRoot(String json)
    {
        JsonElement root;

        try
        {
            root = JsonParser.parseString(json);
        }
        catch (JsonParseException e)
        {
            throw new ProjectConfigException(FILE_NAME + " is not valid JSON: " + e.getMessage(), e);
        }

        if (!root.isJsonObject())
        {
            throw new ProjectConfigException(FILE_NAME + " must contain a JSON object");
        }

        return root.getAsJsonObject();
    }

    private static List<String> readPatterns(JsonObject root, String key)
    {
        JsonElement value = root.get(key);

        if ((value == null) || value.isJsonNull())
        {
            return null;
        }

        if (!value.isJsonArray())
        {
            throw new ProjectConfigException(key + " must be an array of patterns");
        }

        JsonArray patternArray = value.getAsJsonArray();

        List<String> patterns = new ArrayList<>();

        for (JsonElement pattern : patternArray)
        {
            if (!isString(pattern))
            {
                throw new ProjectConfigException(key + " must contain only pattern strings");
            }

            String patternText = pattern.getAsString().trim();

            if (!patternText.isEmpty())
            {
                patterns.add(patternText);
            }
        }

        if (patterns.isEmpty())
        {
            throw new ProjectConfigException(key + " must declare at least one pattern");
        }

        return patterns;
    }

    private static Set<TestAnnotation> readTestAnnotations(JsonObject root)
    {
        JsonElement value = root.get("testAnnotations");

        if ((value == null) || value.isJsonNull())
        {
            return null;
        }

        if (!value.isJsonArray())
        {
            throw new ProjectConfigException(
                "testAnnotations must be an array of " + sortedKeys(TEST_ANNOTATIONS));
        }

        Set<TestAnnotation> testAnnotations = EnumSet.noneOf(TestAnnotation.class);

        for (JsonElement annotation : value.getAsJsonArray())
        {
            if (!isString(annotation))
            {
                throw new ProjectConfigException(
                    "testAnnotations must contain only " + sortedKeys(TEST_ANNOTATIONS));
            }

            TestAnnotation testAnnotation = TEST_ANNOTATIONS.get(annotation.getAsString().toLowerCase(Locale.ROOT));

            if (testAnnotation == null)
            {
                throw new ProjectConfigException(
                    "Unknown test framework " + annotation.getAsString() + ", expected one of "
                        + sortedKeys(TEST_ANNOTATIONS));
            }

            testAnnotations.add(testAnnotation);
        }

        return testAnnotations;
    }

    private static CapitalizationScheme readEnum(
        JsonObject root, String key, Map<String, CapitalizationScheme> allowed
    )
    {
        JsonElement value = root.get(key);

        if ((value == null) || value.isJsonNull())
        {
            return null;
        }

        if (!isString(value))
        {
            throw new ProjectConfigException(key + " must be one of " + sortedKeys(allowed));
        }

        CapitalizationScheme scheme = allowed.get(value.getAsString());

        if (scheme == null)
        {
            throw new ProjectConfigException(
                key + " is " + value.getAsString() + ", expected one of " + sortedKeys(allowed));
        }

        return scheme;
    }

    private static Boolean readBoolean(JsonObject root, String key)
    {
        JsonElement value = root.get(key);

        if ((value == null) || value.isJsonNull())
        {
            return null;
        }

        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
        {
            throw new ProjectConfigException(key + " must be true or false");
        }

        return value.getAsBoolean();
    }

    private static boolean isString(JsonElement element)
    {
        return element.isJsonPrimitive() && ((JsonPrimitive) element).isString();
    }

    private static String sortedKeys(Map<String, ?> allowed)
    {
        return String.join(", ", new TreeSet<>(allowed.keySet()));
    }
}
