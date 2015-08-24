package com.github.kelemen.brazier.actions;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.utils.ExceptionHelper;

public interface UndoAction {
    public static final UndoAction DO_NOTHING = () -> { };

    public void undo();

    public static UndoAction toIdempotent(UndoAction action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        AtomicReference<UndoAction> actionRef = new AtomicReference<>(action);
        return () -> {
            UndoAction currentAction = actionRef.getAndSet(null);
            if (currentAction != null) {
                currentAction.undo();
            }
        };
    }
}
