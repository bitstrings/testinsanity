package org.bitstrings.idea.plugins.testinsanity.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ProjectConfigSchemaTest
{
    private static final String SCHEMA_RESOURCE = "/schema/testinsanity.schema.json";

    private static final List<String> DECLARED_KEYS =
        List.of(
            "$schema", "schemes", "testClassPatterns", "testMethodPatterns", "capitalizeSubject",
            "testAnnotations", "additionalTestAnnotations", "includeInheritedMethods",
            "includeInterfacesAndAbstracts", "includeNestedClasses", "syncDisplayName", "refactoring",
            "navigation", "gutterIcons", "preselectRenames");

    @Test
    public void schema_resource_shipsWithThePlugin()
    {
        assertNotNull(ProjectConfigSchemaTest.class.getResource(SCHEMA_RESOURCE));
    }

    @Test
    public void schema_declaredKeys_matchTheKeysTheParserKnows()
    {
        assertEquals(
            new TreeSet<>(DECLARED_KEYS).toString(),
            new TreeSet<>(schemaProperties().keySet()).toString());
    }

    @Test
    public void schema_root_forbidsUnknownKeys()
    {
        assertFalse(schema().get("additionalProperties").getAsBoolean());
    }

    @Test
    public void schema_schemeObject_isClosedAndRequiresEveryKey()
    {
        JsonObject scheme = schemaProperties().getAsJsonObject("schemes").getAsJsonObject("items");

        assertEquals(
            new TreeSet<>(List.of("name", "testClass", "testMethods")).toString(),
            new TreeSet<>(scheme.getAsJsonObject("properties").keySet()).toString());
        assertFalse(scheme.get("additionalProperties").getAsBoolean());
        assertEquals(3, scheme.getAsJsonArray("required").size());
    }

    @Test
    public void schema_documentedExamples_areAcceptedByTheParser()
    {
        for (Map.Entry<String, JsonElement> property : schemaProperties().entrySet())
        {
            if ("$schema".equals(property.getKey()))
            {
                continue;
            }

            for (String value : documentedValues(property.getValue().getAsJsonObject()))
            {
                String json = "{\"" + property.getKey() + "\":" + value + "}";

                assertEquals(json, List.of(), ProjectConfigParser.parse(json).getWarnings());
            }
        }
    }

    private static List<String> documentedValues(JsonObject property)
    {
        String type = property.get("type").getAsString();

        if ("boolean".equals(type))
        {
            return List.of("true", "false");
        }

        if ("string".equals(type))
        {
            return quotedValues(property.getAsJsonArray("enum"));
        }

        JsonObject items = property.getAsJsonObject("items");

        if ("object".equals(items.get("type").getAsString()))
        {
            return schemeValues(items.getAsJsonObject("properties"));
        }

        List<String> values = new ArrayList<>();

        for (String value : quotedValues(documentedItems(items)))
        {
            values.add("[" + value + "]");
        }

        return values;
    }

    private static JsonArray documentedItems(JsonObject items)
    {
        return items.has("examples") ? items.getAsJsonArray("examples") : items.getAsJsonArray("enum");
    }

    private static List<String> schemeValues(JsonObject schemeProperties)
    {
        List<String> names = quotedValues(schemeProperties.getAsJsonObject("name").getAsJsonArray("examples"));
        List<String> classes =
            quotedValues(schemeProperties.getAsJsonObject("testClass").getAsJsonArray("examples"));
        List<String> methods =
            quotedValues(
                schemeProperties.getAsJsonObject("testMethods").getAsJsonObject("items").getAsJsonArray("examples"));

        List<String> values = new ArrayList<>();

        for (int index = 0; index < names.size(); index++)
        {
            values
                .add(
                    "[{\"name\":" + names.get(index)
                        + ",\"testClass\":" + classes.get(index % classes.size())
                        + ",\"testMethods\":[" + methods.get(index % methods.size()) + "]}]");
        }

        return values;
    }

    private static List<String> quotedValues(JsonArray values)
    {
        List<String> quoted = new ArrayList<>();

        for (JsonElement value : values)
        {
            quoted.add("\"" + value.getAsString() + "\"");
        }

        return quoted;
    }

    private static JsonObject schemaProperties()
    {
        return schema().getAsJsonObject("properties");
    }

    private static JsonObject schema()
    {
        try (InputStream stream = ProjectConfigSchemaTest.class.getResourceAsStream(SCHEMA_RESOURCE))
        {
            assertNotNull(stream);

            return JsonParser
                .parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                .getAsJsonObject();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("The bundled schema could not be read", e);
        }
    }
}
