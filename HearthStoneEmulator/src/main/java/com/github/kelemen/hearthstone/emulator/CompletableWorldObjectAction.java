package com.github.kelemen.hearthstone.emulator;

public interface CompletableWorldObjectAction<T> {
    public CompleteWorldObjectAction<T> startAlterWorld(World world, T object);
}
