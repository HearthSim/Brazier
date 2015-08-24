package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.PlayerProperty;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.parsing.NamedArg;
import java.util.function.Predicate;
import org.jtrim.utils.ExceptionHelper;

public final class TargetedActionConditions {
    public static <Actor, Target> TargetedActionCondition<Actor, Target> forActor(
            @NamedArg("filter") Predicate<? super Actor> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        return (World world, Actor actor, Target target) -> {
            return filter.test(actor);
        };
    }

    public static <Actor, Target> TargetedActionCondition<Actor, Target> forTarget(
            @NamedArg("filter") Predicate<? super Target> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        return (World world, Actor actor, Target target) -> {
            return filter.test(target);
        };
    }

    public static <Actor extends PlayerProperty, Target extends PlayerProperty> TargetedActionCondition<Actor, Target> sameOwner() {
        return (World world, Actor actor, Target target) -> {
            return actor.getOwner() == target.getOwner();
        };
    }

    private TargetedActionConditions() {
        throw new AssertionError();
    }
}
