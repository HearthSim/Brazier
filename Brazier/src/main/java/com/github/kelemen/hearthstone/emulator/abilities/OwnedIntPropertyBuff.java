package com.github.kelemen.hearthstone.emulator.abilities;

public interface OwnedIntPropertyBuff<T> {
    public static final OwnedIntPropertyBuff<Object> IDENTITY = (owner, prev) -> prev;

    public int buffProperty(T owner, int prevValue);
}
