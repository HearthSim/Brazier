package com.github.kelemen.brazier;

import com.github.kelemen.brazier.actions.PlayTargetRequest;
import com.github.kelemen.brazier.actions.UndoAction;
import org.jtrim.utils.ExceptionHelper;

public final class PlayerAgent {
    private final WorldPlayAgent playAgent;
    private final PlayerId playerId;

    public PlayerAgent(WorldPlayAgent playAgent, PlayerId playerId) {
        ExceptionHelper.checkNotNullArgument(playAgent, "playAgent");
        ExceptionHelper.checkNotNullArgument(playerId, "playerId");

        this.playAgent = playAgent;
        this.playerId = playerId;
    }

    public Player getPlayer() {
        return playAgent.getWorld().getPlayer(playerId);
    }

    public UndoAction attack(TargetId attacker, TargetId defender) {
        // TODO: Check the validity of the move.
        return playAgent.attack(attacker, defender);
    }

    public UndoAction playNonMinionCard(int cardIndex) {
        return playCard(cardIndex, -1, null);
    }

    public UndoAction playNonMinionCard(int cardIndex, TargetId target) {
        return playCard(cardIndex, -1, target);
    }

    public UndoAction playMinionCard(int cardIndex, int minionLocation) {
        ExceptionHelper.checkArgumentInRange(minionLocation, 0, Integer.MAX_VALUE, "minionLocation");

        return playCard(cardIndex, minionLocation, null);
    }

    public UndoAction playMinionCard(int cardIndex, int minionLocation, TargetId target) {
        ExceptionHelper.checkArgumentInRange(minionLocation, 0, Integer.MAX_VALUE, "minionLocation");
        ExceptionHelper.checkNotNullArgument(target, "target");

        return playCard(cardIndex, minionLocation, target);
    }

    private UndoAction playCard(int cardIndex, int minionLocation, TargetId target) {
        // TODO: Check the validity of the move
        return playAgent.playCard(cardIndex, new PlayTargetRequest(playerId, minionLocation, target));
    }
}
