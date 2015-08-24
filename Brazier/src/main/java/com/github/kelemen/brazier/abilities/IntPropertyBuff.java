package com.github.kelemen.brazier.abilities;

public interface IntPropertyBuff {
    public static final IntPropertyBuff IDENTITY = (prev) -> prev;

    public int buffProperty(int prevValue);
}
