package org.bitstrings.idea.plugins.testinsanity;

import java.util.LinkedHashMap;
import java.util.Map;

import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.rename.naming.AutomaticRenamer;
import com.intellij.refactoring.rename.naming.NameSuggester;

class AutomaticTestRenamer
    extends AutomaticRenamer
{
    private final Map<PsiNamedElement, String> requestedNames = new LinkedHashMap<>();

    private final TestRenameKind renameKind;

    private final boolean selectedByDefault;

    AutomaticTestRenamer(TestRenameKind renameKind, boolean selectedByDefault)
    {
        this.renameKind = renameKind;
        this.selectedByDefault = selectedByDefault;
    }

    void addElement(PsiNamedElement element, String newName)
    {
        if (element.getName() == null)
        {
            return;
        }

        requestedNames.put(element, newName);

        myElements.add(element);

        suggestAllNames(element.getName(), newName);
    }

    @Override
    protected String suggestNameForElement(
        PsiNamedElement element, NameSuggester suggester, String newClassName, String oldClassName
    )
    {
        String requestedName = requestedNames.get(element);

        return (requestedName == null)
            ? element.getName()
            : requestedName;
    }

    @Override
    public boolean isSelectedByDefault()
    {
        return selectedByDefault;
    }

    @Override
    public String getDialogTitle()
    {
        return renameKind.message("dialog.title");
    }

    @Override
    public String getDialogDescription()
    {
        return renameKind.message("dialog.description");
    }

    @Override
    public String entityName()
    {
        return renameKind.message("dialog.entityname");
    }
}
