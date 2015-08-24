package com.github.kelemen.brazier.cards;

import com.github.kelemen.brazier.TargetableCharacter;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.actions.PlayArg;
import com.github.kelemen.brazier.actions.TargetedAction;
import com.github.kelemen.brazier.actions.UndoAction;
import java.util.Optional;

public interface PlayAction<Actor> extends TargetedAction<Actor, Optional<TargetableCharacter>> {
    public default UndoAction doPlay(World world, PlayArg<Actor> arg) {
        return alterWorld(world, arg.getActor(), arg.getTarget());
    }

    public static <Actor> PlayAction<Actor> doNothing() {
        return (World world, Actor actor, Optional<TargetableCharacter> target) -> {
            return UndoAction.DO_NOTHING;
        };
    }
}
