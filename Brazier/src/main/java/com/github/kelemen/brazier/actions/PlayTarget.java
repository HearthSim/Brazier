package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.TargetableCharacter;
import org.jtrim.utils.ExceptionHelper;

public final class PlayTarget {
    private final Player castingPlayer;
    private final TargetableCharacter target;

    public PlayTarget(Player castingPlayer, TargetableCharacter target) {
        ExceptionHelper.checkNotNullArgument(castingPlayer, "castingPlayer");

        this.castingPlayer = castingPlayer;
        this.target = target;
    }

    public Player getCastingPlayer() {
        return castingPlayer;
    }

    public TargetableCharacter getTarget() {
        return target;
    }
}
