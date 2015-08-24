package com.github.kelemen.brazier;

import com.github.kelemen.brazier.actions.UndoableUnregisterRef;

public interface UndoableRegistry {
    public UndoableUnregisterRef register();
}
