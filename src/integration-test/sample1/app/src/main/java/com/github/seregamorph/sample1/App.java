package com.github.seregamorph.sample1;

import com.github.seregamorph.sample1.core.ListUtils;
import java.util.List;

public class App {

    public static void main(String[] args) {
        System.out.println(getDefaultList());
    }

    static List<String> getDefaultList() {
        return ListUtils.toStringList(List.of(1, 2, 3));
    }
}
