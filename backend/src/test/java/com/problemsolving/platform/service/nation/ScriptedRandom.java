package com.problemsolving.platform.service.nation;

import java.util.Random;

/**
 * Test-only Random: replays scripted values so cache rolls are deterministic.
 * Extra calls repeat the last scripted value, so short scripts also work for long loops.
 */
public class ScriptedRandom extends Random {

    private final double[] doubles;
    private final int[] ints;
    private int doubleIndex;
    private int intIndex;

    public ScriptedRandom(double[] doubles) {
        this(doubles, new int[0]);
    }

    public ScriptedRandom(double[] doubles, int[] ints) {
        this.doubles = doubles;
        this.ints = ints;
    }

    @Override
    public double nextDouble() {
        if (doubles.length == 0) {
            throw new AssertionError("unexpected nextDouble() call");
        }
        double value = doubles[Math.min(doubleIndex, doubles.length - 1)];
        doubleIndex++;
        return value;
    }

    @Override
    public int nextInt(int bound) {
        if (ints.length == 0) {
            throw new AssertionError("unexpected nextInt(" + bound + ") call");
        }
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        int value = ints[Math.min(intIndex, ints.length - 1)];
        intIndex++;
        if (value >= bound) {
            throw new AssertionError("scripted int " + value + " out of bound " + bound);
        }
        return value;
    }
}
