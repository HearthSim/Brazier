package com.github.kelemen.brazier.actions;

import org.jtrim.utils.ExceptionHelper;

public final class PlayArg<Actor> {
    private final Actor actor;
    private final PlayTarget target;

    public PlayArg(Actor actor, PlayTarget target) {
        ExceptionHelper.checkNotNullArgument(actor, "actor");
        ExceptionHelper.checkNotNullArgument(target, "target");

        this.actor = actor;
        this.target = target;
    }

    public Actor getActor() {
        return actor;
    }

    public PlayTarget getTarget() {
        return target;
    }
}
