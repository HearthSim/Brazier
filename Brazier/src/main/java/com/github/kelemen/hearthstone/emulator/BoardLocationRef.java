package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.UndoAction;

public interface BoardLocationRef extends MinionRef {
    public BoardLocationRef tryGetLeft();
    public BoardLocationRef tryGetRight();

    public boolean isOnBoard();

    public UndoAction removeFromBoard();
    public UndoAction destroy();
}
