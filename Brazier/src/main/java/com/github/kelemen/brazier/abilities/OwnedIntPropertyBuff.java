package com.github.kelemen.brazier.abilities;

public interface OwnedIntPropertyBuff<T> {
    public static final OwnedIntPropertyBuff<Object> IDENTITY = (owner, prev) -> prev;

    public int buffProperty(T owner, int prevValue);
}
