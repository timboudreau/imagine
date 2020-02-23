/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt.util;

import static java.lang.Double.doubleToLongBits;
import static java.lang.Float.floatToIntBits;
import java.util.Arrays;

/**
 * Builds good hash codes.
 *
 * @author Tim Boudreau
 */
public final class Hasher {

    private long hash = 0;
    private int cursor = 0;
    private static final int[] PRIMES = new int[]{
        2_311, 4_877, 5_237, 8_089, 10_079, 13_219, 16_547,
        17_519, 19_211, 7, 149
    };

    private int nextPrime() {
        return PRIMES[cursor++ % PRIMES.length];
    }

    public Hasher add(long val) {
        hash = nextPrime() * hash + val;
        return this;
    }

    public Hasher add(double val) {
        return add(doubleToLongBits(val));
    }

    public Hasher add(int val) {
        hash = nextPrime() * hash + val;
        return this;
    }

    public Hasher add(float val) {
        return add(floatToIntBits(val));
    }

    public Hasher add(byte val) {
        return add(val & 0xFF);
    }

    public Hasher add(int[] values) {
        for (int i = 0; i < values.length; i++) {
            add(values[i]);
        }
        return this;
    }

    public Hasher add(byte[] values) {
        add(Arrays.hashCode(values));
        return this;
    }

    public Hasher add(long[] values) {
        for (int i = 0; i < values.length; i++) {
            add(values[i]);
        }
        return this;
    }

    @Override
    public int hashCode() {
        return (((int) hash) ^ ((int) (hash >> 32)));
    }
}
