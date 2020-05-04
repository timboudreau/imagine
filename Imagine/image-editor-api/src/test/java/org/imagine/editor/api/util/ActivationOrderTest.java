/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.editor.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ActivationOrderTest {

    @Test
    public void testSomeMethod() {
        ActivationOrder<String> ord = new ActivationOrder<>(5, String::hashCode);
        List<String> got = new ArrayList<>();
        ActivationOrder.ActivationSequenceBuilder<String> bldr = ord.sequenceBuilder();
        bldr.add("A", () -> {
            got.add("A");
        });
        bldr.add("B", () -> {
            got.add("B");
        });
        bldr.add("C", () -> {
            got.add("C");
        });
        Runnable runAll = bldr.build();
        ord.activated("C");
        ord.activated("B");
        ord.activated("A");
        runAll.run();
        assertEquals(Arrays.asList("A", "B", "C"), got, "abc");

        got.clear();
        ord.activated("C");
        runAll.run();
        assertEquals(Arrays.asList("C", "A", "B"), got, "cab");

        got.clear();
        ord.activated("B");
        runAll.run();
        assertEquals(Arrays.asList("B", "C", "A"), got, "bca");

        got.clear();
        ord.activated("C");
        runAll.run();
        assertEquals(Arrays.asList("C", "B", "A"), got, "cba");

        got.clear();
        ord.activated("A");
        runAll.run();
        assertEquals(Arrays.asList("A", "C", "B"), got, "acb");

        got.clear();
        ord.activated("A");
        runAll.run();
        assertEquals(Arrays.asList("A", "C", "B"), got, "acb no change");

        got.clear();
        ord.prejudice("C");
        runAll.run();
        assertEquals(Arrays.asList("A", "B", "C"), got, "abc predj c");

        got.clear();
        ord.prejudice("B");
        runAll.run();
        assertEquals(Arrays.asList("A", "C", "B"), got, "acb predj b");

        got.clear();
        ord.prejudice("A");
        runAll.run();
        assertEquals(Arrays.asList("C", "B", "A"), got, "cba predj b");
    }
}
