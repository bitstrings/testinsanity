package org.bitstrings.idea.plugins.testinsanity.util;

import java.util.regex.Pattern;

public final class TestAnnotationPattern
{
    public static final String PACKAGE_WILDCARD_SUFFIX = ".*";

    private static final Pattern QUALIFIED_NAME =
        Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*"
            + "(\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");

    private TestAnnotationPattern()
    {
    }

    public static boolean isValid(String annotationPattern)
    {
        if (annotationPattern == null)
        {
            return false;
        }

        String qualifiedName =
            annotationPattern.endsWith(PACKAGE_WILDCARD_SUFFIX)
                ? annotationPattern.substring(0, annotationPattern.length() - PACKAGE_WILDCARD_SUFFIX.length())
                : annotationPattern;

        return !qualifiedName.contains("*") && QUALIFIED_NAME.matcher(qualifiedName).matches();
    }
}
