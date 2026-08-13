package org.bitstrings.idea.plugins.testinsanity.config;

import java.util.List;

import org.bitstrings.idea.plugins.testinsanity.RenameTestService;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;

public final class ProjectConfigWatcher
    implements BulkFileListener
{
    private final Project project;

    public ProjectConfigWatcher(Project project)
    {
        this.project = project;
    }

    @Override
    public void after(List<? extends VFileEvent> events)
    {
        ProjectConfigService configService = ProjectConfigService.getInstance(project);

        for (VFileEvent event : events)
        {
            if (configService.isConfigFile(event.getPath()))
            {
                configService.invalidate();

                ApplicationManager.getApplication().invokeLater(this::reload, project.getDisposed());

                return;
            }
        }
    }

    private void reload()
    {
        RenameTestService.getInstance(project).update();
    }
}
