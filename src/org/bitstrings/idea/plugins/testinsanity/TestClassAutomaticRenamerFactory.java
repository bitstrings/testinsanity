package org.bitstrings.idea.plugins.testinsanity;

import java.util.Collection;
import java.util.Map;

import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.bitstrings.idea.plugins.testinsanity.lang.TestElementAdapters;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.naming.AutomaticRenamer;
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory;
import com.intellij.usageView.UsageInfo;

public class TestClassAutomaticRenamerFactory
    implements AutomaticRenamerFactory
{
    private static final String ENABLED_PROPERTY = "testinsanity.rename.test.classes";

    @Override
    public boolean isApplicable(PsiElement element)
    {
        return TestElementAdapters.isClass(element)
            && TestInsanitySettings.getInstance(element.getProject()).isRefactoringEnabled();
    }

    @Override
    public String getOptionName()
    {
        return TestRenameKind.CLASS.message("option.name");
    }

    @Override
    public boolean isEnabled()
    {
        return PropertiesComponent.getInstance().getBoolean(ENABLED_PROPERTY, true);
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        PropertiesComponent.getInstance().setValue(ENABLED_PROPERTY, enabled, true);
    }

    @Override
    public AutomaticRenamer createRenamer(PsiElement element, String newName, Collection<UsageInfo> usages)
    {
        Project project = element.getProject();

        AutomaticTestRenamer renamer =
            new AutomaticTestRenamer(
                TestRenameKind.CLASS, TestInsanitySettings.getInstance(project).isRenamingDialogEnabled());

        PsiClass elementClass = TestElementAdapters.asClass(element, element.getResolveScope());

        if (elementClass == null)
        {
            return renamer;
        }

        RenameTestService renameTestService = RenameTestService.getInstance(project);

        Map<PsiClass, String> renames =
            (renameTestService.getTestClassSiblingMediator().resolveTestClass(elementClass) == null)
                ? renameTestService.renameSubjectClassMapping(elementClass, newName)
                : renameTestService.renameTestClassMapping(elementClass, newName);

        renames.forEach(renamer::addElement);

        return renamer;
    }
}
