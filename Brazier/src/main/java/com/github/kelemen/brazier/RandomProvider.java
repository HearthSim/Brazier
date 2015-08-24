package com.github.kelemen.brazier;

public interface RandomProvider {
    public int roll(int bound);

    public default int roll(int minValue, int maxValue) {
        if (minValue == maxValue) {
            return minValue;
        }

        return minValue + roll(maxValue - minValue + 1);
    };
}
