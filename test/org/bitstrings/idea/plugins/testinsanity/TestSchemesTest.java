package org.bitstrings.idea.plugins.testinsanity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bitstrings.idea.plugins.testinsanity.util.TestPatternMatcher.CapitalizationScheme;
import org.junit.Test;

public class TestSchemesTest
{
    @Test
    public void generateTestClassName_eachScheme_usesItsOwnClassPattern()
    {
        TestSchemes schemes = unitAndIntegrationSchemes();

        assertEquals("ColorTest", schemes.generateTestClassName(schemes.getSchemes().get(0), "Color"));
        assertEquals("ColorIT", schemes.generateTestClassName(schemes.getSchemes().get(1), "Color"));
    }

    @Test
    public void generateTestMethodName_eachScheme_usesItsOwnMethodPattern()
    {
        TestSchemes schemes = unitAndIntegrationSchemes();

        assertEquals("testIsDark", schemes.generateTestMethodName(schemes.getSchemes().get(0), "isDark"));
        assertEquals("isDark_test", schemes.generateTestMethodName(schemes.getSchemes().get(1), "isDark"));
    }

    @Test
    public void renameTestClassName_nameMatchingTheSecondScheme_renamesThroughThatScheme()
    {
        TestSchemes schemes = unitAndIntegrationSchemes();

        String renamed = schemes.renameTestClassName("ColorIT", "Color", "Paint");

        assertEquals("PaintIT", renamed);
    }

    @Test
    public void renameTestClassName_nameMatchingNoScheme_isLeftAlone()
    {
        TestSchemes schemes = unitAndIntegrationSchemes();

        String renamed = schemes.renameTestClassName("ColorSpec", "Color", "Paint");

        assertEquals("ColorSpec", renamed);
    }

    @Test
    public void renameSubjectClassNameXXX_testNameMatchingNoScheme_keepsTheNewTestName()
    {
        TestSchemes schemes = unitAndIntegrationSchemes();

        String renamed = schemes.renameSubjectClassNameXXX("Color", "ColorSpec", "PaintSpec");

        assertEquals("PaintSpec", renamed);
    }

    @Test
    public void renameSubjectClassNameXXX_testNameMatchingAScheme_stripsTheSuffix()
    {
        TestSchemes schemes = unitAndIntegrationSchemes();

        String renamed = schemes.renameSubjectClassNameXXX("Color", "ColorIT", "PaintIT");

        assertEquals("Paint", renamed);
    }

    @Test
    public void generateTestClassName_emptySchemeList_hasNoSchemeToAsk()
    {
        TestSchemes schemes = new TestSchemes(List.of());

        assertEquals(List.of(), schemes.getSchemes());
        assertNull(schemes.findSubjectMethodName(null, "testIsDark"));
    }

    @Test
    public void validatePatterns_wellFormedSchemes_areAccepted()
    {
        unitAndIntegrationSchemes().validatePatterns();
    }

    static TestSchemes unitAndIntegrationSchemes()
    {
        return new TestSchemes(
            List.of(
                scheme("unit", "${className}Test", "test${subjectName}*"),
                scheme("it", "${className}IT", "${subjectName}_test")));
    }

    static TestScheme scheme(String name, String classPattern, String methodPattern)
    {
        List<TestMethodSiblingMediator> methodMediators = new ArrayList<>();

        methodMediators
            .add(
                new PatternBasedTestMethodSiblingMediator(
                    methodPattern, CapitalizationScheme.IF_PREFIXED, Set.of(), true, true));

        return new TestScheme(
            name,
            new PatternBasedTestClassSiblingMediator(classPattern, false),
            new CompositeTestMethodSiblingMediator(methodMediators));
    }
}
