package com.github.seregamorph.maven.halflife.graph;

import java.util.Locale;

enum Scope {
    COMPILE,
    TEST;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
