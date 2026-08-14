package org.bitstrings.idea.plugins.testinsanity.actions;

import javax.swing.Icon;

import org.bitstrings.idea.plugins.testinsanity.TestInsanityBundle;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanityConfiguration;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanityConfiguration.Key;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.util.IconLoader;

public class TestInsanityEnablerAction
    extends ToggleAction
{
    private static final Icon ON = IconLoader.getIcon("/icons/menu_on_icon.svg", TestInsanityEnablerAction.class);
    private static final Icon OFF = IconLoader.getIcon("/icons/menu_off_icon.svg", TestInsanityEnablerAction.class);

    @Override
    public boolean isSelected(AnActionEvent event)
    {
        return (event.getProject() != null)
            && TestInsanityConfiguration.getInstance(event.getProject()).isRefactoringEnabled();
    }

    @Override
    public void setSelected(AnActionEvent event, boolean state)
    {
        if (event.getProject() != null)
        {
            TestInsanitySettings.getInstance(event.getProject()).setRefactoringEnabled(state);
        }
    }

    @Override
    public void update(AnActionEvent event)
    {
        super.update(event);

        if (event.getProject() == null)
        {
            return;
        }

        TestInsanityConfiguration configuration = TestInsanityConfiguration.getInstance(event.getProject());

        event.getPresentation().setEnabled(!configuration.isGovernedByProjectConfig(Key.REFACTORING));
        event.getPresentation().setIcon(configuration.isRefactoringEnabled() ? ON : OFF);
        event.getPresentation().setText(TestInsanityBundle.message("testinsanity.action.enabler.title"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.EDT;
    }
}
