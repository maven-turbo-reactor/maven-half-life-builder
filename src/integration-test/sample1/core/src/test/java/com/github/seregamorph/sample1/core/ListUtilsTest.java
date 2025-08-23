package com.github.seregamorph.sample1.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.seregamorph.sample1.test.utils.TestUtils;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListUtilsTest {

    @Test
    public void shouldConvertList() {
        assertEquals("[3, 2, 1]", TestUtils.toString(ListUtils.toStringList(List.of(3L, 2L, 1L))));
    }
}
