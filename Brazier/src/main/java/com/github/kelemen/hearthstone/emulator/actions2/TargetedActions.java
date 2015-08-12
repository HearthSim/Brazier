package com.github.kelemen.hearthstone.emulator.actions2;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.UndoBuilder;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import org.jtrim.utils.ExceptionHelper;

public final class TargetedActions {
    public static <Target> TargetedAction<Card, Target> withCardsMinion(
            @NamedArg("action") TargetedAction<? super Minion, ? super Target> action) {
        return forActors(EntitySelectors.actorCardsMinion(), action);
    }

    public static <Actor, Target, FinalTarget> TargetedAction<Actor, Target> forTargets(
            @NamedArg("selector") EntitySelector<? super Actor, ? super Target, ? extends FinalTarget> selector,
            @NamedArg("action") TargetedAction<? super Actor, ? super FinalTarget> action) {
        ExceptionHelper.checkNotNullArgument(selector, "selector");
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, Actor actor, Target initialTarget) -> {
            UndoBuilder result = new UndoBuilder();
            selector.select(world, actor, initialTarget).forEach((FinalTarget target) -> {
                result.addUndo(action.alterWorld(world, actor, target));
            });
            return result;
        };
    }

    public static <Actor, Target, FinalActor> TargetedAction<Actor, Target> forActors(
            @NamedArg("selector") EntitySelector<? super Actor, ? super Target, ? extends FinalActor> selector,
            @NamedArg("action") TargetedAction<? super FinalActor, ? super Target> action) {
        ExceptionHelper.checkNotNullArgument(selector, "selector");
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, Actor initialActor, Target target) -> {
            UndoBuilder result = new UndoBuilder();
            selector.select(world, initialActor, target).forEach((FinalActor actor) -> {
                result.addUndo(action.alterWorld(world, actor, target));
            });
            return result;
        };
    }

    private TargetedActions() {
        throw new AssertionError();
    }
}
