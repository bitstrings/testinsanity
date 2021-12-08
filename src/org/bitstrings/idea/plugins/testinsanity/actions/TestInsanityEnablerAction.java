package org.bitstrings.idea.plugins.testinsanity.actions;

import javax.swing.Icon;

import org.bitstrings.idea.plugins.testinsanity.TestInsanityBundle;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.IconLoader;

public class TestInsanityEnablerAction
    extends AnAction
{
    private static final Icon ON = IconLoader.getIcon("/icons/menu_on_icon.svg", TestInsanityEnablerAction.class);
    private static final Icon ON_SELECTED = ON;
    private static final Icon OFF = IconLoader.getIcon("/icons/menu_off_icon.svg", TestInsanityEnablerAction.class);

    @Override
    public void actionPerformed(AnActionEvent event)
    {
        TestInsanitySettings settings = TestInsanitySettings.getInstance(event.getProject());

        settings.setRefactoringEnabled(!settings.isRefactoringEnabled());
    }

    @Override
    public void update(AnActionEvent event)
    {
        if (event.getProject() == null)
        {
            return;
        }

        boolean refactoringEnabled = TestInsanitySettings.getInstance(event.getProject()).isRefactoringEnabled();

        event.getPresentation().setIcon(refactoringEnabled ? ON : OFF);
        event.getPresentation().setSelectedIcon(refactoringEnabled ? ON_SELECTED : OFF);
        event.getPresentation().setText(TestInsanityBundle.message("testinsanity.action.enabler.title"));
    }
}
