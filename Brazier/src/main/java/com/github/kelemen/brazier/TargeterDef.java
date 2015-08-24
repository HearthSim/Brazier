package com.github.kelemen.brazier;

import java.util.Objects;
import org.jtrim.utils.ExceptionHelper;

public final class TargeterDef {
    private final PlayerId playerId;
    private final boolean hero;
    private final boolean directAttack;

    public TargeterDef(PlayerId playerId, boolean hero, boolean directAttack) {
        ExceptionHelper.checkNotNullArgument(playerId, "playerId");

        this.playerId = playerId;
        this.hero = hero;
        this.directAttack = directAttack;
    }

    public boolean hasSameOwner(PlayerProperty property) {
        return Objects.equals(playerId, property.getOwner().getPlayerId());
    }

    public PlayerId getPlayerId() {
        return playerId;
    }

    public boolean isHero() {
        return hero;
    }

    public boolean isDirectAttack() {
        return directAttack;
    }
}
