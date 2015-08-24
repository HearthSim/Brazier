package com.github.kelemen.brazier.event;

import com.github.kelemen.brazier.World;

public interface CompletableWorldObjectAction<T> {
    public CompleteWorldObjectAction<T> startAlterWorld(World world, T object);
}
