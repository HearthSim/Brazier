package com.github.kelemen.brazier;

import com.github.kelemen.brazier.actions.UndoAction;

public interface BoardReservationRef extends MinionRef {
    public UndoAction showMinion();
}
