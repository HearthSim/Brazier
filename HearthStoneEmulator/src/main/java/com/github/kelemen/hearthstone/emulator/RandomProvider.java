package com.github.kelemen.hearthstone.emulator;

public interface RandomProvider {
    public int roll(int bound);

    public default int roll(int minValue, int maxValue) {
        if (minValue == maxValue) {
            return minValue;
        }

        return minValue + roll(maxValue - minValue + 1);
    };
}
