package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import org.jtrim.utils.ExceptionHelper;

public final class UndoableResult<T> implements UndoAction {
    private final T result;
    private final UndoAction undoAction;

    public UndoableResult(T result) {
        this(result, UndoAction.DO_NOTHING);
    }

    public UndoableResult(T result, UndoAction undoAction) {
        ExceptionHelper.checkNotNullArgument(undoAction, "undoAction");

        this.result = result;
        this.undoAction = undoAction;
    }

    public T getResult() {
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
