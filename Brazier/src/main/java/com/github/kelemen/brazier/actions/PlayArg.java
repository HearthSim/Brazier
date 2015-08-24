package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.TargetableCharacter;
import java.util.Optional;
import org.jtrim.utils.ExceptionHelper;

public final class PlayArg<Actor> {
    private final Actor actor;
    private final Optional<TargetableCharacter> target;

    public PlayArg(Actor actor, TargetableCharacter target) {
        this(actor, Optional.ofNullable(target));
    }

    public PlayArg(Actor actor, Optional<TargetableCharacter> target) {
        ExceptionHelper.checkNotNullArgument(actor, "actor");
        ExceptionHelper.checkNotNullArgument(target, "target");

        this.actor = actor;
        this.target = target;
    }

    public Actor getActor() {
        return actor;
    }

    public Optional<TargetableCharacter> getTarget() {
        return target;
    }
}
