package com.github.kelemen.brazier.event;

import com.github.kelemen.brazier.UndoableResult;
import com.github.kelemen.brazier.actions.UndoableAction;

public interface CompletableWorldActionEvents<T> extends CompletableWorldActionEventsRegistry<T> {
    public default UndoableResult<UndoableAction> triggerEvent(T object) {
        return triggerEvent(true, object);
    }

    public UndoableResult<UndoableAction> triggerEvent(boolean delayable, T object);
}
