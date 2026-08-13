package org.bitstrings.idea.plugins.testinsanity.config;

import org.bitstrings.idea.plugins.testinsanity.TestInsanityBundle;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory;
import com.jetbrains.jsonSchema.extension.SchemaType;
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion;

public class ProjectConfigSchemaProvider
    implements JsonSchemaFileProvider
{
    private static final String SCHEMA_RESOURCE = "/schema/testinsanity.schema.json";

    private final Project project;

    public ProjectConfigSchemaProvider(Project project)
    {
        this.project = project;
    }

    @Override
    public boolean isAvailable(VirtualFile file)
    {
        return ProjectConfigService.getInstance(project).isConfigFile(file.getPath());
    }

    @Override
    public String getName()
    {
        return TestInsanityBundle.message("testinsanity.config.schema.name");
    }

    @Override
    public VirtualFile getSchemaFile()
    {
        return JsonSchemaProviderFactory.getResourceFile(ProjectConfigSchemaProvider.class, SCHEMA_RESOURCE);
    }

    @Override
    public SchemaType getSchemaType()
    {
        return SchemaType.embeddedSchema;
    }

    @Override
    public JsonSchemaVersion getSchemaVersion()
    {
        return JsonSchemaVersion.SCHEMA_7;
    }
}
