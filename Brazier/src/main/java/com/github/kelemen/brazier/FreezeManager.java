package com.github.kelemen.brazier;

import com.github.kelemen.brazier.actions.UndoAction;

public final class FreezeManager implements Silencable {
    private boolean frozen;

    public FreezeManager() {
        this(false);
    }

    public FreezeManager(boolean frozen) {
        this.frozen = frozen;
    }

    public FreezeManager copy() {
        return new FreezeManager(frozen);
    }

    public boolean isFrozen() {
        return frozen;
    }

    public UndoAction endTurn(int numberOfAttacks) {
        if (numberOfAttacks > 0 || !frozen) {
            return UndoAction.DO_NOTHING;
        }

        frozen = false;
        return () -> frozen = true;
    }

    public UndoAction freeze() {
        if (frozen) {
            return UndoAction.DO_NOTHING;
        }

        return setFrozen(true);
    }

    @Override
    public UndoAction silence() {
        return setFrozen(false);
    }

    private UndoAction setFrozen(boolean newValue) {
        if (frozen == newValue) {
            return UndoAction.DO_NOTHING;
        }

        frozen = newValue;
        return () -> frozen = !newValue;
    }
}
