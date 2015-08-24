package com.github.kelemen.brazier;

import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoableAction;
import org.jtrim.utils.ExceptionHelper;

public final class PreparedResult<T> {
    private final T result;
    private final UndoableAction activateAction;

    public PreparedResult(T result, UndoableAction activateAction) {
        ExceptionHelper.checkNotNullArgument(result, "result");
        ExceptionHelper.checkNotNullArgument(activateAction, "activateAction");

        this.result = result;
        this.activateAction = activateAction;
    }

    public UndoAction activate() {
        return activateAction.doAction();
    }

    public T getResult() {
        return result;
    }
}
