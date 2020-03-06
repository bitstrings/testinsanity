/*
 *=============================================================================
 *                      THIS FILE AND ITS CONTENTS ARE THE
 *                    EXCLUSIVE AND CONFIDENTIAL PROPERTY OF
 *
 *                          EXPRETIO TECHNOLOGIES, INC.
 *
 * Any unauthorized use of this file or any of its parts, including, but not
 * limited to, viewing, editing, copying, compiling, and distributing, is
 * strictly prohibited.
 *
 * Copyright ExPretio Technologies, Inc., 2020. All rights reserved.
 *=============================================================================
 */
package org.bitstrings.idea.plugins.testinsanity;

import static org.bitstrings.idea.plugins.testinsanity.util.TestInsanityUtil.getLightClassMethod;

import org.jetbrains.kotlin.psi.KtNamedFunction;

import com.intellij.codeInsight.PsiEquivalenceUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.rename.naming.AutomaticRenamer;

class AutomaticTestRenamer
    extends AutomaticRenamer
{
    public AutomaticTestRenamer()
    {
    }

    public AutomaticTestRenamer(PsiNamedElement element, String newName)
    {
        PsiNamedElement elementToRename =
            element instanceof KtNamedFunction
                ? getLightClassMethod((KtNamedFunction) element)
                : element;

        Project project = element.getProject();

        RenameTestService renameTestService = RenameTestService.getInstance(project);

        renameTestService
            .renameSubjectMethodMapping(
                (PsiMethod) element, newName, GlobalSearchScope.projectScope(project))
            .forEach(
                (rename, renameNewName) ->
                {
                    if (!PsiEquivalenceUtil.areElementsEquivalent(rename, elementToRename))
                    {
                        myElements.add((PsiMethod) rename);
                        suggestAllNames(((PsiMethod) rename).getName(), renameNewName);
                    }
                }
            );
    }

    public void addElement(PsiNamedElement element, String newName)
    {
        myElements.add(element);
        suggestAllNames(element.getName(), newName);
    }

    @Override
    public boolean isSelectedByDefault()
    {
        return true;
    }

    @Override
    public String getDialogTitle()
    {
        return TestInsanityBundle.message("testinsanity.renamer.dialog.title");
    }

    @Override
    public String getDialogDescription()
    {
        return TestInsanityBundle.message("testinsanity.renamer.dialog.description");
    }

    @Override
    public String entityName()
    {
        return TestInsanityBundle.message("testinsanity.renamer.dialog.entityname");
    }

}
