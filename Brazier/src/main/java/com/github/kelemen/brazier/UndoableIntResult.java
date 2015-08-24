package com.github.kelemen.brazier;

import com.github.kelemen.brazier.actions.UndoAction;
import org.jtrim.utils.ExceptionHelper;

public final class UndoableIntResult implements UndoAction {
    public static final UndoableIntResult ZERO = new UndoableIntResult(0, DO_NOTHING);

    private final int result;
    private final UndoAction undoAction;

    public UndoableIntResult(int result, UndoAction undoAction) {
        ExceptionHelper.checkNotNullArgument(undoAction, "undoAction");

        this.result = result;
        this.undoAction = undoAction;
    }

    public int getResult() {
        return result;
    }

    public UndoAction getUndoAction() {
        return undoAction;
    }

    @Override
    public void undo() {
        undoAction.undo();
    }
}
