package org.bitstrings.idea.plugins.testinsanity;

public final class TestScheme
{
    private final String name;

    private final TestClassSiblingMediator classMediator;

    private final TestMethodSiblingMediator methodMediator;

    public TestScheme(String name, TestClassSiblingMediator classMediator, TestMethodSiblingMediator methodMediator)
    {
        this.name = name;
        this.classMediator = classMediator;
        this.methodMediator = methodMediator;
    }

    public String getName()
    {
        return name;
    }

    public TestClassSiblingMediator getClassMediator()
    {
        return classMediator;
    }

    public TestMethodSiblingMediator getMethodMediator()
    {
        return methodMediator;
    }
}
