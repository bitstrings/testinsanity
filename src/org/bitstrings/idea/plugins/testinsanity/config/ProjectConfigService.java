package org.bitstrings.idea.plugins.testinsanity.config;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.bitstrings.idea.plugins.testinsanity.TestInsanityBundle;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;

public final class ProjectConfigService
{
    private static final String NOTIFICATION_GROUP = "TestInsanity";

    private final Project project;

    private final AtomicReference<ProjectConfig> config = new AtomicReference<>();

    public ProjectConfigService(Project project)
    {
        this.project = project;
    }

    public static ProjectConfigService getInstance(Project project)
    {
        return project.getService(ProjectConfigService.class);
    }

    public ProjectConfig getConfig()
    {
        ProjectConfig currentConfig = config.get();

        if (currentConfig == null)
        {
            currentConfig = load();

            config.set(currentConfig);
        }

        return currentConfig;
    }

    public void invalidate()
    {
        config.set(null);
    }

    public VirtualFile findConfigFile()
    {
        String basePath = project.getBasePath();

        return (basePath == null)
            ? null
            : LocalFileSystem.getInstance().findFileByPath(basePath + "/" + ProjectConfigParser.FILE_NAME);
    }

    public boolean isConfigFile(String path)
    {
        String basePath = project.getBasePath();

        return (basePath != null) && path.equals(basePath + "/" + ProjectConfigParser.FILE_NAME);
    }

    private ProjectConfig load()
    {
        VirtualFile configFile = findConfigFile();

        if (configFile == null)
        {
            return ProjectConfig.ABSENT;
        }

        try
        {
            ProjectConfig loadedConfig = ProjectConfigParser.parse(VfsUtilCore.loadText(configFile));

            for (String warning : loadedConfig.getWarnings())
            {
                notify(warning, NotificationType.WARNING);
            }

            return loadedConfig;
        }
        catch (ProjectConfigException e)
        {
            notify(e.getMessage(), NotificationType.ERROR);
        }
        catch (IOException e)
        {
            notify(
                TestInsanityBundle.message("testinsanity.config.unreadable", ProjectConfigParser.FILE_NAME),
                NotificationType.ERROR);
        }

        return ProjectConfig.ABSENT;
    }

    private void notify(String content, NotificationType type)
    {
        NotificationGroupManager
            .getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification(TestInsanityBundle.message("testinsanity.config.title"), content, type)
            .notify(project);
    }
}
