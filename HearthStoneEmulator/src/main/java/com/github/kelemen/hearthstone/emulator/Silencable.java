package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.UndoAction;

public interface Silencable {
    public UndoAction silence();
}
