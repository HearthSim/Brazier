package com.github.kelemen.brazier;

public interface CompletableWorldObjectAction<T> {
    public CompleteWorldObjectAction<T> startAlterWorld(World world, T object);
}
