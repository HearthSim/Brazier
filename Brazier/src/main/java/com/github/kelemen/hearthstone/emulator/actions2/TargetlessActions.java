package com.github.kelemen.hearthstone.emulator.actions2;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.UndoBuilder;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import org.jtrim.utils.ExceptionHelper;

public final class TargetlessActions {
    public static <Actor, FinalActor> TargetlessAction<Actor> forActors(
            @NamedArg("actors") EntitySelector<? super Actor, ? extends FinalActor> actors,
            @NamedArg("action") TargetlessAction<? super FinalActor> action) {
        ExceptionHelper.checkNotNullArgument(actors, "actors");
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, Actor initialActor) -> {
            UndoBuilder result = new UndoBuilder();
            actors.select(world, initialActor).forEach((FinalActor actor) -> {
                result.addUndo(action.alterWorld(world, actor));
            });
            return result;
        };
    }

    private TargetlessActions() {
        throw new AssertionError();
    }
}
