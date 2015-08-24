package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.minions.Minion;
import org.jtrim.utils.ExceptionHelper;

public final class BattleCryArg {
    private final Minion source;
    private final PlayTarget target;

    public BattleCryArg(Minion source, PlayTarget target) {
        ExceptionHelper.checkNotNullArgument(source, "source");
        ExceptionHelper.checkNotNullArgument(target, "target");

        this.source = source;
        this.target = target;
    }

    public Minion getSource() {
        return source;
    }

    public PlayTarget getTarget() {
        return target;
    }

    public Player getCastingPlayer() {
        return target.getCastingPlayer();
    }
}
