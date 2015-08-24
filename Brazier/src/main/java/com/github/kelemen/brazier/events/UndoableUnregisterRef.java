package com.github.kelemen.brazier.events;

import com.github.kelemen.brazier.actions.UndoAction;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jtrim.utils.ExceptionHelper;

public interface UndoableUnregisterRef extends UndoAction {
    public static final UndoableUnregisterRef UNREGISTERED_REF = () -> UndoAction.DO_NOTHING;

    /**
     * Removes the registered object, so that it no longer does what it
     * was registered for. This method must be idempotent. That is, calling it
     * multiple times should have no additional effects.
     *
     * @return an {@code UndoAction} which can be used to restore the
     *   registration to its previous state. This method never returns
     *   {@code null}.
     */
    public UndoAction unregister();

    @Override
    public default void undo() {
        unregister();
    }

    public static UndoableUnregisterRef makeIdempotent(UndoableUnregisterRef wrapped) {
        ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");
        return new UndoableUnregisterRef() {
            private final AtomicBoolean unregistered = new AtomicBoolean(false);

            @Override
            public UndoAction unregister() {
                if (unregistered.compareAndSet(false, true)) {
                    UndoAction unregisterUndo = wrapped.unregister();
                    return () -> {
                        unregisterUndo.undo();
                        unregistered.set(false);
                    };
                }
                else {
                    return UndoAction.DO_NOTHING;
                }
            }

            @Override
            public void undo() {
                if (unregistered.get()) {
                    throw new IllegalStateException("Cannot be undone after unregistering the reference.");
                }
                wrapped.undo();
            }
        };
    }
}
