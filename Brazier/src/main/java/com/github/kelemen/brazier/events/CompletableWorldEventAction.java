package com.github.kelemen.brazier.event;

import com.github.kelemen.brazier.PlayerProperty;
import com.github.kelemen.brazier.World;

public interface CompletableWorldEventAction<Self extends PlayerProperty, EventSource> {
    public CompleteWorldEventAction<Self, EventSource> startEvent(World world, Self self, EventSource eventSource);
}
