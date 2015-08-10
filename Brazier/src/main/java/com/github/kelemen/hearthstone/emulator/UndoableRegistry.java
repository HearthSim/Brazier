package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRef;

public interface UndoableRegistry {
    public UndoableUnregisterRef register();
}
