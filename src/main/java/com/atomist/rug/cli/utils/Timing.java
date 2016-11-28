package com.atomist.rug.cli.utils;

public class Timing {

    private long start = System.currentTimeMillis();

    public float duration() {
        return (System.currentTimeMillis() - start) / 1000F;
    }

}
