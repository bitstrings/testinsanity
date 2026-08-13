package org.bitstrings.idea.plugins.testinsanity;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.bitstrings.idea.plugins.testinsanity.lang.TestElementAdapters;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.scope.ProjectFilesScope;
import com.intellij.refactoring.rename.RenameJavaClassProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.containers.MultiMap;

public class RenameTestClassProcessor
    extends RenameJavaClassProcessor
{
    @Override
    public boolean canProcessElement(PsiElement element)
    {
        return (TestElementAdapters.isClass(element)
            && TestInsanitySettings.getInstance(element.getProject()).isRefactoringEnabled());
    }

    @Override
    public void prepareRenaming(PsiElement element, String newName, Map<PsiElement, String> allRenames)
    {
        if (StringUtils.isEmpty(newName))
        {
            return;
        }

        PsiClass elementClass = TestElementAdapters.asClass(element, element.getResolveScope());

        if (elementClass == null)
        {
            return;
        }

        RenameTestService renameTestService = RenameTestService.getInstance(element.getProject());

        GlobalSearchScope searchScope = renameTestService.getSearchScope(elementClass, ProjectFilesScope.INSTANCE);

        allRenames.putAll(
            renameTestService.renameTestClassMapping(elementClass, newName, searchScope)
        );
    }

    @Override
    public void findExistingNameConflicts(
        PsiElement element, String newName, MultiMap<PsiElement, String> conflicts, Map<PsiElement, String> allRenames
    )
    {
        allRenames.forEach(
            (renameElement, renameNewName) ->
            {
                if (renameElement instanceof PsiClass)
                {
                    super.findExistingNameConflicts(renameElement, renameNewName, conflicts, allRenames);
                }
            }
        );
    }

    @Override
    public void findCollisions(
        PsiElement element, String newName, Map<? extends PsiElement, String> allRenames, List<UsageInfo> result
    )
    {
        allRenames.forEach(
            (renameElement, renameNewName) ->
            {
                if (renameElement instanceof PsiClass)
                {
                    super.findCollisions(renameElement, renameNewName, allRenames, result);
                }
            }
        );
    }
}
