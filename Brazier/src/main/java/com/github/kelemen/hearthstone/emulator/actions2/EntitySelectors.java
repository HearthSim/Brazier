package com.github.kelemen.hearthstone.emulator.actions2;

import com.github.kelemen.hearthstone.emulator.BoardLocationRef;
import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.SummonLocationRef;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import java.util.stream.Stream;

public final class EntitySelectors {
    public static <Actor, Target, Selection> EntitySelector<Actor, Target, Selection> empty() {
        return (World world, Actor actor, Target target) -> Stream.empty();
    }

    public static <Actor, Target> EntitySelector<Actor, Target, Actor> self() {
        return (World world, Actor actor, Target target) -> Stream.of(actor);
    }

    public static <Target> EntitySelector<Card, Target, Minion> actorCardsMinion() {
        return (World world, Card actor, Target target) -> {
            Minion minion = actor.getMinion();
            return minion != null ? Stream.of(minion) : Stream.empty();
        };
    }

    public static <Actor, Target extends PlayerProperty> EntitySelector<Actor, Target, Player> targetsOwnerPlayer() {
        return (World world, Actor actor, Target target) -> Stream.of(target.getOwner());
    }

    public static <Actor, Target extends PlayerProperty> EntitySelector<Actor, Target, Hero> targetsHero() {
        return (World world, Actor actor, Target target) -> Stream.of(target.getOwner().getHero());
    }

    public static <Actor, Target extends Minion> EntitySelector<Actor, Target, Minion> targetsNeighbours() {
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

    private EntitySelectors() {
        throw new AssertionError();
    }
}
