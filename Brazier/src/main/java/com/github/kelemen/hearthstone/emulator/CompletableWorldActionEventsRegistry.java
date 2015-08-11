package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRef;

public interface CompletableWorldActionEventsRegistry<T> {
    public default UndoableUnregisterRef addListener(CompletableWorldObjectAction<? super T> listener) {
        return addListener(Priorities.NORMAL_PRIORITY, listener);
    }

    public UndoableUnregisterRef addListener(int priority, CompletableWorldObjectAction<? super T> listener);
}
