package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.Priorities;

public interface WorldActionEventsRegistry<T> {
    public default UndoableUnregisterRef addAction(WorldObjectAction<T> action) {
        return addAction(Priorities.NORMAL_PRIORITY, action);
    }

    public UndoableUnregisterRef addAction(int priority, WorldObjectAction<T> action);
}
