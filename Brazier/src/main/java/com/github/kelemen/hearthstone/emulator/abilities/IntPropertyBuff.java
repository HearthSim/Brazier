package com.github.kelemen.hearthstone.emulator.abilities;

public interface IntPropertyBuff {
    public static final IntPropertyBuff IDENTITY = (prev) -> prev;

    public int buffProperty(int prevValue);
}
