package com.github.kelemen.brazier.event;

import com.github.kelemen.brazier.Priorities;
import com.github.kelemen.brazier.actions.UndoableUnregisterRef;

public interface CompletableWorldActionEventsRegistry<T> {
    public default UndoableUnregisterRef addListener(CompletableWorldObjectAction<? super T> listener) {
        return addListener(Priorities.NORMAL_PRIORITY, listener);
    }

    public UndoableUnregisterRef addListener(int priority, CompletableWorldObjectAction<? super T> listener);
}
