package com.github.kelemen.brazier.actions;

public interface WorldActionQueue {
    public UndoAction queueAction(WorldAction action);
}
