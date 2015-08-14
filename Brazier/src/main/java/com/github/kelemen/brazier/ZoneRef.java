package com.github.kelemen.brazier;

import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoableAction;
import org.jtrim.utils.ExceptionHelper;

public final class ZoneRef {
    private Zone currentZone;
    private UndoableAction removeAction;

    public ZoneRef() {
        this.currentZone = Zone.SET_ASIDE;
        this.removeAction = UndoableAction.DO_NOTHING;
    }

    public UndoAction setAside() {
        return moveToZone(Zone.SET_ASIDE, UndoableAction.DO_NOTHING);
    }

    public UndoAction moveToZone(Zone newZone, UndoableAction newRemoveAction) {
        ExceptionHelper.checkNotNullArgument(newZone, "zone");
        ExceptionHelper.checkNotNullArgument(newRemoveAction, "newRemoveAction");

        UndoableAction prevRemoveAction = removeAction;
        removeAction = newRemoveAction;

        UndoAction removeUndo = removeAction.doAction();

        Zone prevZone = currentZone;
        currentZone = newZone;

        return () -> {
            currentZone = prevZone;
            removeUndo.undo();
            removeAction = prevRemoveAction;
        };
    }
}
