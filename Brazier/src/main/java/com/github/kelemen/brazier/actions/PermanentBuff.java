package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRef;

public interface PermanentBuff<Target> {
    public UndoAction buff(World world, Target target);
}
