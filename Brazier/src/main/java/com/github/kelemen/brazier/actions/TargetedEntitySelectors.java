package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.BoardLocationRef;
import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.SummonLocationRef;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.stream.Stream;
import org.jtrim.utils.ExceptionHelper;

public final class TargetedEntitySelectors {
    public static <Actor, Target, Selection> TargetedEntitySelector<Actor, Target, Selection> empty() {
        return (World world, Actor actor, Target target) -> Stream.empty();
    }

    public static <Actor, Target> TargetedEntitySelector<Actor, Target, Actor> self() {
        return (World world, Actor actor, Target target) -> Stream.of(actor);
    }

    public static <Actor, Target, Selection> TargetedEntitySelector<Actor, Target, Selection> filtered(
            @NamedArg("filter") EntityFilter<Selection> filter,
            @NamedArg("selector") TargetedEntitySelector<? super Actor, ? super Target, ? extends Selection> selector) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");
        ExceptionHelper.checkNotNullArgument(selector, "selector");

        return (World world, Actor actor, Target target) -> {
            Stream<? extends Selection> selection = selector.select(world, actor, target);
            return filter.select(world, selection);
        };
    }

    public static <Actor, Target> TargetedEntitySelector<Actor, Target, Target> target() {
        return (World world, Actor actor, Target target) -> Stream.of(target);
    }

    public static <Actor, Target extends PlayerProperty> TargetedEntitySelector<Actor, Target, Player> targetsOwnerPlayer() {
        return (World world, Actor actor, Target target) -> Stream.of(target.getOwner());
    }

    public static <Actor, Target extends PlayerProperty> TargetedEntitySelector<Actor, Target, Hero> targetsHero() {
        return (World world, Actor actor, Target target) -> Stream.of(target.getOwner().getHero());
    }

    public static <Actor, Target extends Minion> TargetedEntitySelector<Actor, Target, Minion> targetsNeighbours() {
        return (World world, Actor actor, Target target) -> {
            SummonLocationRef locationRef = target.getLocationRef();

            BoardLocationRef leftRef = locationRef.tryGetLeft();
            BoardLocationRef rightRef = locationRef.tryGetRight();

            if (leftRef != null) {
                if (rightRef != null) {
                    return Stream.of(rightRef.getMinion(), leftRef.getMinion());
                }
                else {
                    return Stream.of(leftRef.getMinion());
                }
            }
            else {
                if (rightRef != null) {
                    return Stream.of(rightRef.getMinion());
                }
                else {
                    return Stream.empty();
                }
            }
        };
    }

    public static <Actor, Target extends PlayerProperty> TargetedEntitySelector<Actor, Target, Minion> targetsBoard() {
        return (World world, Actor actor, Target target) -> {
            return target.getOwner().getBoard().getAllMinions().stream();
        };
    }

    private TargetedEntitySelectors() {
        throw new AssertionError();
    }
}
