package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import java.util.Collections;
import java.util.List;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

public final class DeathResolutionResult implements UndoAction {
    public static final DeathResolutionResult NO_DEATHS = new DeathResolutionResult(false, DO_NOTHING);

    private final List<PlayerId> deadPlayers;
    private final UndoAction undoAction;
    private final boolean deathOccurred;

    public DeathResolutionResult(boolean deathOccurred, UndoAction undoAction) {
        this(deathOccurred, Collections.emptyList(), undoAction);
    }

    public DeathResolutionResult(boolean deathOccurred, List<PlayerId> deadPlayers, UndoAction undoAction) {
        this.deadPlayers = CollectionsEx.readOnlyCopy(deadPlayers);
        this.undoAction = undoAction;
        this.deathOccurred = deathOccurred;

        ExceptionHelper.checkNotNullElements(this.deadPlayers, "deadPlayers");
    }

    public List<PlayerId> getDeadPlayers() {
        return deadPlayers;
    }

    public UndoAction getUndoAction() {
        return undoAction;
    }

    public boolean deathOccurred() {
        return deathOccurred;
    }

    public boolean isGameOver() {
        return !deadPlayers.isEmpty();
    }

    @Override
    public void undo() {
        undoAction.undo();
    }
}
