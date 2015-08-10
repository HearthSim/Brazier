package com.github.kelemen.hearthstone.emulator.abilities;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;

public interface ActiveAura {
    public UndoAction updateAura(World world);
    public UndoAction deactivate();
}
