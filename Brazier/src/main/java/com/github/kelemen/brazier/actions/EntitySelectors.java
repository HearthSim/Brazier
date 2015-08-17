package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.stream.Stream;
import org.jtrim.utils.ExceptionHelper;

import static com.github.kelemen.brazier.actions.EntityFilters.*;

public final class EntitySelectors {
    public static <Actor, Selection> EntitySelector<Actor, Selection> empty() {
        return (World world, Actor actor) -> Stream.empty();
    }

    public static <Actor, Selection> EntitySelector<Actor, Selection> filtered(
            @NamedArg("filter") EntityFilter<Selection> filter,
            @NamedArg("selector") EntitySelector<? super Actor, ? extends Selection> selector) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");
        ExceptionHelper.checkNotNullArgument(selector, "selector");

        return (World world, Actor actor) -> {
            Stream<? extends Selection> selection = selector.select(world, actor);
            return filter.select(world, selection);
        };
    }

    public static <Actor extends PlayerProperty, Selection> EntitySelector<Actor, Selection> fromOpponent(
            @NamedArg("selector") EntitySelector<? super Player, ? extends Selection> selector) {
        ExceptionHelper.checkNotNullArgument(selector, "selector");
        return (World world, Actor actor) -> {
            return selector.select(world, actor.getOwner());
        };
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Hero> friendlyHero() {
        return (World world, Actor actor) -> Stream.of(actor.getOwner().getHero());
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Card> friendlyHand() {
        return (World world, Actor actor) -> actor.getOwner().getHand().getCards().stream();
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Card> friendlyDeck() {
        return (World world, Actor actor) -> actor.getOwner().getBoard().getDeck().getCards().stream();
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Minion> friendlyBoard() {
        return (World world, Actor actor) -> actor.getOwner().getBoard().getAllMinions().stream();
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Minion> friendlyBoardBuffable() {
        return filtered(fromPredicate(buffableMinions()), friendlyBoard());
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Minion> friendlyBoardAlive() {
        return filtered(fromPredicate(aliveTargets()), friendlyBoard());
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Hero> enemyHero() {
        return fromOpponent(friendlyHero());
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Card> enemyHand() {
        return fromOpponent(friendlyHand());
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Card> enemyDeck() {
        return fromOpponent(friendlyDeck());
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Minion> enemyBoard() {
        return fromOpponent(friendlyBoard());
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Minion> enemyBoardBuffable() {
        return fromOpponent(friendlyBoardBuffable());
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Minion> enemyBoardAlive() {
        return fromOpponent(friendlyBoardAlive());
    }

    private EntitySelectors() {
        throw new AssertionError();
    }
}
