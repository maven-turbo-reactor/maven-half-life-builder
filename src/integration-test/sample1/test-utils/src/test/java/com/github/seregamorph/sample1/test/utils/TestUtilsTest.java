package com.github.seregamorph.sample1.test.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TestUtilsTest {

    @Test
    public void shouldToString() {
        assertEquals("null", TestUtils.toString(null));
        assertEquals("1", TestUtils.toString(1));
    }
}
