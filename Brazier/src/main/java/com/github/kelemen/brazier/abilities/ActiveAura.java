package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.actions.UndoAction;

public interface ActiveAura {
    public UndoAction updateAura(World world);
    public UndoAction deactivate();
}
