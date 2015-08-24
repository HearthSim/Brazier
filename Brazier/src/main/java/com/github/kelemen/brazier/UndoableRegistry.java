package com.github.kelemen.brazier;

import com.github.kelemen.brazier.events.UndoableUnregisterRef;

public interface UndoableRegistry {
    public UndoableUnregisterRef register();
}
