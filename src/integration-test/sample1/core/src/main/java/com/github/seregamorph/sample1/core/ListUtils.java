package com.github.seregamorph.sample1.core;

import java.util.List;
import java.util.Objects;

public final class ListUtils {

    public static List<String> toStringList(List<?> list) {
        return list.stream()
            .map(Objects::toString)
            .toList();
    }

    private ListUtils() {
    }
}
