package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.UndoAction;

public interface DestroyableEntity extends BornEntity {
    public UndoAction scheduleToDestroy();
    public boolean isScheduledToDestroy();
    public UndoAction destroy();
}
