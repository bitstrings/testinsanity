package org.bitstrings.idea.plugins.testinsanity;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.jetbrains.kotlin.psi.KtClass;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.psi.search.PsiSearchScopeUtil;
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
        boolean canProcessElement = ((element instanceof PsiClass) || (element instanceof KtClass));

        Project project = element.getProject();

        RenameTestService renameTestService = RenameTestService.getInstance(project);

        GlobalSearchScope searchScope = renameTestService.getSearchScope(element, ProjectFilesScope.INSTANCE);

        GlobalSearchScope testSearchScope = searchScope.intersectWith(GlobalSearchScopes.projectTestScope(project));

        return (canProcessElement && PsiSearchScopeUtil.isInScope(testSearchScope, element)
            && TestInsanitySettings.getInstance(project).isRefactoringEnabled());
    }

    @Override
    public void prepareRenaming(PsiElement element, String newName, Map<PsiElement, String> allRenames)
    {
        if (StringUtils.isEmpty(newName))
        {
            return;
        }

        Project project = element.getProject();

        if (element instanceof KtClass)
        {
            element = JavaPsiFacade.getInstance(project)
                .findClass(((KtClass) element).getFqName().asString(), element.getResolveScope());
        }

        RenameTestService renameTestService = RenameTestService.getInstance(project);

        GlobalSearchScope searchScope = renameTestService.getSearchScope(element, ProjectFilesScope.INSTANCE);

        allRenames.putAll(
            renameTestService.renameTestClassMapping((PsiClass) element, newName, searchScope)
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
