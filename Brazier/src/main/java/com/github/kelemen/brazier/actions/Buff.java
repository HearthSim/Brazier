package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRef;

public interface Buff<Target> {
    public UndoableUnregisterRef buff(World world, Target target);
}
