package com.github.seregamorph.maven.halflife;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.seregamorph.maven.halflife.graph.ProjectPart;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderKeyTest {

    @Test
    void shouldOrderKeys() {
        var k1 = new OrderKey(ProjectPart.MAIN, 0);
        var k2 = new OrderKey(ProjectPart.TEST, 0);
        var k3 = new OrderKey(ProjectPart.MAIN, 1);
        var k4 = new OrderKey(ProjectPart.TEST, 1);
        var k5 = new OrderKey(ProjectPart.MAIN, 2);
        var k6 = new OrderKey(ProjectPart.TEST, 2);
        var k7 = new OrderKey(ProjectPart.MAIN, null);
        var k8 = new OrderKey(ProjectPart.TEST, null);
        var list = Arrays.asList(k1, k2, k3, k4, k5, k6, k7, k8);
        Collections.shuffle(list);
        Collections.sort(list);
        assertEquals(List.of(k1, k2, k3, k4, k5, k6, k7, k8), list);
    }
}
