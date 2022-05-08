package com.pranav.javacompletion.parser;

import org.openjdk.source.tree.LineMap;

/** Utility methods for working with {@link LineMap}. */
public final class LineMapUtil {
    private LineMapUtil() {}

    public static int getPositionFromZeroBasedLineAndColumn(LineMap lineMap, int line, int column) {
        // LineMap accepts 1-based line and column numbers.
        return (int) lineMap.getPosition(line + 1, column + 1);
    }
}
