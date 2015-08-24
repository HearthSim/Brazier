package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.PlayerId;
import com.github.kelemen.brazier.TargetId;
import com.github.kelemen.brazier.cards.CardDescr;
import org.jtrim.utils.ExceptionHelper;

public final class PlayTargetRequest {
    private final PlayerId castingPlayerId;

    private final int minionLocation;
    private final TargetId targetId;

    private final CardDescr choseOneChoice;

    public PlayTargetRequest(PlayerId castingPlayerId) {
        this(castingPlayerId, -1, null);
    }

    public PlayTargetRequest(
            PlayerId castingPlayerId,
            int minionLocation,
            TargetId targetId) {
        this(castingPlayerId, minionLocation, targetId, null);
    }

    public PlayTargetRequest(
            PlayerId castingPlayerId,
            int minionLocation,
            TargetId targetId,
            CardDescr choseOneChoice) {
        ExceptionHelper.checkNotNullArgument(castingPlayerId, "castingPlayerId");

        this.castingPlayerId = castingPlayerId;
        this.minionLocation = minionLocation;
        this.targetId = targetId;
        this.choseOneChoice = choseOneChoice;
    }

    public PlayerId getCastingPlayerId() {
        return castingPlayerId;
    }

    public int getMinionLocation() {
        return minionLocation;
    }

    public TargetId getTargetId() {
        return targetId;
    }

    public CardDescr getChoseOneChoice() {
        return choseOneChoice;
    }
}
