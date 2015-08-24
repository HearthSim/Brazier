package com.github.kelemen.brazier.events;

import com.github.kelemen.brazier.actions.UndoAction;

public interface WorldActionEvents<T> extends WorldActionEventsRegistry<T> {
    public default UndoAction triggerEvent(T object) {
        return triggerEvent(true, object);
    }

    public UndoAction triggerEvent(boolean delayable, T object);
}
