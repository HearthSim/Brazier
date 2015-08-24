package com.github.kelemen.brazier;

public final class Priorities {
    public static final int LOWEST_PRIORITY = Integer.MIN_VALUE;
    public static final int LOW_PRIORITY = -1;
    public static final int NORMAL_PRIORITY = 0;
    public static final int HIGH_PRIORITY = 1;
    public static final int HIGHEST_PRIORITY = Integer.MAX_VALUE;

    private Priorities() {
        throw new AssertionError();
    }
}
