package com.github.seregamorph.sample1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    public void shouldGetDefaultList() {
        assertEquals(List.of("1", "2", "3"), App.getDefaultList());
    }
}
