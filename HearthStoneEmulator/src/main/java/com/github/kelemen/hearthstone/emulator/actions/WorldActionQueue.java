package com.github.kelemen.hearthstone.emulator.actions;

public interface WorldActionQueue {
    public UndoAction queueAction(WorldAction action);
}
