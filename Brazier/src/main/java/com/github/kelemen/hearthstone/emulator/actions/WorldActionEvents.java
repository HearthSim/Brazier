package com.github.kelemen.hearthstone.emulator.actions;

public interface WorldActionEvents<T> extends WorldActionEventsRegistry<T> {
    public default UndoAction triggerEvent(T object) {
        return triggerEvent(true, object);
    }

    public UndoAction triggerEvent(boolean delayable, T object);
}
