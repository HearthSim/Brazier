package com.github.kelemen.hearthstone.emulator;

public interface CompletableWorldEventAction<Self extends PlayerProperty, EventSource> {
    public CompleteWorldEventAction<Self, EventSource> startEvent(World world, Self self, EventSource eventSource);
}
