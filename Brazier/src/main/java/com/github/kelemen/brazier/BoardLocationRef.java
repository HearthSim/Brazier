package com.github.kelemen.brazier;

import com.github.kelemen.brazier.actions.UndoAction;

public interface BoardLocationRef extends MinionRef {
    public BoardLocationRef tryGetLeft();
    public BoardLocationRef tryGetRight();

    public boolean isOnBoard();

    public UndoAction removeFromBoard();
    public UndoAction destroy();
}
