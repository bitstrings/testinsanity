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

import org.bitstrings.idea.plugins.testinsanity.lang.TestElementAdapters;

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
        PsiMethod elementMethod = TestElementAdapters.asMethod(element);

        if (elementMethod == null)
        {
            return;
        }

        Project project = element.getProject();

        RenameTestService.getInstance(project)
            .renameSubjectMethodMapping(
                elementMethod, newName, GlobalSearchScope.projectScope(project))
            .forEach(
                (rename, renameNewName) ->
                {
                    if (!PsiEquivalenceUtil.areElementsEquivalent(rename, elementMethod))
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
