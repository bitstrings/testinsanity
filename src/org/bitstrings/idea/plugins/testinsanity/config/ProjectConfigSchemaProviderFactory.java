package org.bitstrings.idea.plugins.testinsanity.config;

import static java.util.Collections.singletonList;

import java.util.List;

import com.intellij.openapi.project.Project;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory;

public class ProjectConfigSchemaProviderFactory
    implements JsonSchemaProviderFactory
{
    @Override
    public List<JsonSchemaFileProvider> getProviders(Project project)
    {
        return singletonList(new ProjectConfigSchemaProvider(project));
    }
}
