package com.github.kelemen.brazier.actions;

import org.jtrim.utils.ExceptionHelper;

public final class UndoBuilder implements UndoAction {
    private UndoAction[] undos;
    private int count;

    public UndoBuilder() {
        this(10);
    }

    public UndoBuilder(int expectedSize) {
        this.undos = new UndoAction[expectedSize];
        this.count = 0;
    }

    public void addUndo(UndoAction undo) {
        ExceptionHelper.checkNotNullArgument(undo, "undo");

        if (undo == UndoAction.DO_NOTHING) {
            // Minor optimization
            return;
        }

        if (undos.length >= count) {
            int newLength = Math.max(count + 1, 2 * count);
            UndoAction[] newUndos = new UndoAction[newLength];
            System.arraycopy(undos, 0, newUndos, 0, count);
            undos = newUndos;
        }

        undos[count] = undo;
        count++;
    }

    @Override
    public void undo() {
        for (int i = count - 1; i >= 0; i--) {
            undos[i].undo();
        }
    }
}
