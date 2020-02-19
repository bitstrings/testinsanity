package org.bitstrings.idea.plugins.testinsanity;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanitySettings;
import org.jetbrains.kotlin.asJava.LightClassUtil;
import org.jetbrains.kotlin.psi.KtFunction;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.psi.search.PsiSearchScopeUtil;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.scope.ProjectFilesScope;
import com.intellij.refactoring.rename.RenameJavaMethodProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.containers.MultiMap;

public class RenameTestMethodProcessor
    extends RenameJavaMethodProcessor
{
    @Override
    public boolean canProcessElement(PsiElement element)
    {
        return (((element instanceof PsiMethod) || (element instanceof KtFunction))
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
            element = LightClassUtil.INSTANCE.getLightClassMethod((KtNamedFunction) element);
        }

        Project project = element.getProject();

        RenameTestService renameTestService = RenameTestService.getInstance(project);

        PsiClass elementClass = (PsiClass) element.getParent();

        GlobalSearchScope searchScope = renameTestService.getSearchScope(element, ProjectFilesScope.INSTANCE);

        GlobalSearchScope testSearchScope = searchScope.intersectWith(GlobalSearchScopes.projectTestScope(project));

        if (PsiSearchScopeUtil.isInScope(testSearchScope, element))
        {
            allRenames.putAll(
                renameTestService.renameTestMethodMapping((PsiMethod) element, newName, searchScope)
            );
        }
        else if (
            !renameTestService
                .getTestClassSiblingMediator()
                .getTestClasses(elementClass, searchScope)
                .isEmpty()
        )
        {
            allRenames.putAll(
                renameTestService.renameSubjectMethodMapping((PsiMethod) element, newName, searchScope)
            );
        }
    }

    @Override
    public void findExistingNameConflicts(
        PsiElement element, String newName, MultiMap<PsiElement, String> conflicts, Map<PsiElement, String> allRenames
    )
    {
        allRenames.forEach(
            (renameElement, renameNewName) ->
            {
                if (renameElement instanceof PsiMethod)
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
                if (renameElement instanceof PsiMethod)
                {
                    super.findCollisions(renameElement, renameNewName, allRenames, result);
                }
            }
        );
    }
}
