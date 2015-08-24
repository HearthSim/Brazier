package com.github.kelemen.brazier;

import com.github.kelemen.brazier.actions.UndoAction;

public interface DestroyableEntity extends BornEntity {
    public UndoAction scheduleToDestroy();
    public boolean isScheduledToDestroy();
    public UndoAction destroy();
}
