package com.github.kelemen.brazier;

import java.util.List;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

public final class GameResult {
    private final List<PlayerId> deadPlayers;

    public GameResult(List<PlayerId> deadPlayers) {
        this.deadPlayers = CollectionsEx.readOnlyCopy(deadPlayers);
        ExceptionHelper.checkNotNullElements(this.deadPlayers, "deadPlayers");
    }

    public List<PlayerId> getDeadPlayers() {
        return deadPlayers;
    }

    @Override
    public String toString() {
        return "GameResult{" + "deadPlayers=" + deadPlayers + '}';
    }
}
