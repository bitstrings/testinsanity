package org.bitstrings.idea.plugins.testinsanity;

import static org.bitstrings.idea.plugins.testinsanity.util.KotlinJavaUtil.getLightClassMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.jetbrains.kotlin.psi.KtFunction;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import com.intellij.openapi.application.AppUIExecutor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.psi.search.PsiSearchScopeUtil;
import com.intellij.psi.search.SearchScope;
import com.intellij.refactoring.rename.AutomaticRenamingDialog;
import com.intellij.refactoring.rename.RenameJavaMethodProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.containers.MultiMap;

public class RenameTestMethodProcessor
    extends RenameJavaMethodProcessor
{
    @Override
    public boolean canProcessElement(PsiElement element)
    {
        Project project = element.getProject();

        GlobalSearchScope testSearchScope = GlobalSearchScopes.projectTestScope(project);

        return (((element instanceof PsiMethod) || (element instanceof KtFunction))
            && !PsiSearchScopeUtil.isInScope(testSearchScope, element)
            && TestInsanitySettings.getInstance(element.getProject()).isRefactoringEnabled());
    }

    @Override
    public void prepareRenaming(
        PsiElement element, String newName, Map<PsiElement, String> allRenames, SearchScope scope
    )
    {
        if (StringUtils.isEmpty(newName))
        {
            return;
        }

        if (element instanceof KtNamedFunction)
        {
            element = getLightClassMethod((KtNamedFunction) element);
        }

        super.prepareRenaming(element, newName, allRenames, scope);

        Project project = element.getProject();

        RenameTestService renameTestService = RenameTestService.getInstance(project);

        Map<PsiElement, String> newRenames = new HashMap<>();

        allRenames.forEach(
            (rename, renameNewName) ->
            {
                if (rename instanceof KtNamedFunction)
                {
                    rename = getLightClassMethod((KtNamedFunction) rename);
                }

                newRenames.putAll(
                    renameTestService
                        .renameSubjectMethodMapping(
                            (PsiMethod) rename,
                            renameNewName, GlobalSearchScope.projectScope(project)));
            }
        );

        if (!newRenames.isEmpty() && TestInsanitySettings.getInstance(project).isRenamingDialogEnabled())
        {
            AutomaticTestRenamer renamer = new AutomaticTestRenamer();
            newRenames.forEach(
                (rename, renameNewName) -> renamer.addElement((PsiNamedElement) rename, renameNewName)
            );

            newRenames.clear();

            AppUIExecutor.onUiThread().inSmartMode(project).execute(
                () ->
                {
                    AutomaticRenamingDialog dialog = new AutomaticRenamingDialog(project, renamer);
                    if (dialog.showAndGet())
                    {
                        newRenames.putAll(renamer.getRenames());
                    }
                }
            );
        }

        newRenames.forEach(
            (rename, renameNewName) ->
            {
                super.prepareRenaming(rename, renameNewName, allRenames, scope);
                allRenames.put(rename, renameNewName);
            }
        );
    }

    @Override
    public void findExistingNameConflicts(
        PsiElement element, String newName, MultiMap<PsiElement, String> conflicts, Map<PsiElement, String> allRenames
    )
    {
        super.findExistingNameConflicts(element, newName, conflicts, allRenames);
        allRenames.forEach(
            (rename, renameNewName) -> super.findExistingNameConflicts(rename, renameNewName, conflicts, allRenames));
    }

    @Override
    public void findCollisions(
        PsiElement element, String newName, Map<? extends PsiElement, String> allRenames, List<UsageInfo> result
    )
    {
        super.findCollisions(element, newName, allRenames, result);
        allRenames.forEach((rename, renameNewName) -> super.findCollisions(rename, renameNewName, allRenames, result));
    }
}
