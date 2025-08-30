package com.github.seregamorph.maven.halflife.graph;

import java.util.Locale;

/**
 * @author Sergey Chernov
 */
public enum ProjectPart {
    MAIN,
    TEST;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
