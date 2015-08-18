package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import com.github.kelemen.hearthstone.emulator.weapons.Weapon;
import com.github.kelemen.hearthstone.emulator.weapons.WeaponDescr;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.jtrim.utils.ExceptionHelper;

import static com.github.kelemen.brazier.actions.EntityFilters.*;

public final class EntitySelectors {
    public static <Actor, Selection> EntitySelector<Actor, Selection> empty() {
        return (World world, Actor actor) -> Stream.empty();
    }

    public static <Actor, Target> EntitySelector<Actor, Actor> self() {
        return (World world, Actor actor) -> Stream.of(actor);
    }

    public static <Actor> EntitySelector<Actor, CardDescr> cardsWithKeywords(
            @NamedArg("keywords") Keyword... keywords) {
        Keyword[] keywordsCopy = keywords.clone();
        ExceptionHelper.checkNotNullElements(keywordsCopy, "keywords");

        return (World world, Actor actor) -> {
            List<CardDescr> cards = world.getDb().getCardDb().getByKeywords(keywordsCopy);
            return cards.stream();
        };
    }

    public static <Actor> EntitySelector<Actor, MinionDescr> minionsWithKeywords(
            @NamedArg("keywords") Keyword... keywords) {
        Keyword[] keywordsCopy = keywords.clone();
        ExceptionHelper.checkNotNullElements(keywordsCopy, "keywords");

        return (World world, Actor actor) -> {
            List<MinionDescr> cards = world.getDb().getMinionDb().getByKeywords(keywordsCopy);
            return cards.stream();
        };
    }

    public static <Actor> EntitySelector<Actor, WeaponDescr> weaponsWithKeywords(
            @NamedArg("keywords") Keyword... keywords) {
        Keyword[] keywordsCopy = keywords.clone();
        ExceptionHelper.checkNotNullElements(keywordsCopy, "keywords");

        return (World world, Actor actor) -> {
            List<WeaponDescr> cards = world.getDb().getWeaponDb().getByKeywords(keywordsCopy);
            return cards.stream();
        };
    }

    public static <Actor, Selection> EntitySelector<Actor, Selection> sorted(
            EntitySelector<? super Actor, ? extends Selection> selector,
            Comparator<? super Selection> cmp) {
        ExceptionHelper.checkNotNullArgument(selector, "selector");
        ExceptionHelper.checkNotNullArgument(cmp, "cmp");

        return (World world, Actor actor) -> {
            Stream<? extends Selection> selection = selector.select(world, actor);
            return selection.sorted(cmp);
        };
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

    public static <Actor, Selection> EntitySelector<Actor, Selection> notSelf(
            @NamedArg("selector") EntitySelector<? super Actor, ? extends Selection> selector) {
        ExceptionHelper.checkNotNullArgument(selector, "selector");

        return (World world, Actor actor) -> {
            Stream<? extends Selection> selection = selector.select(world, actor);
            return selection.filter((entity) -> entity != actor);
        };
    }

    public static <Actor extends PlayerProperty, Selection> EntitySelector<Actor, Selection> fromOpponent(
            @NamedArg("selector") EntitySelector<? super Player, ? extends Selection> selector) {
        ExceptionHelper.checkNotNullArgument(selector, "selector");
        return (World world, Actor actor) -> {
            return selector.select(world, actor.getOwner().getOpponent());
        };
    }

    public static EntitySelector<Card, Minion> cardsMinion() {
        return (World world, Card actor) -> {
            Minion minion = actor.getMinion();
            return minion != null ? Stream.of(minion) : Stream.empty();
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
        return filtered(fromPredicate(buffableMinion()), friendlyBoard());
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Minion> friendlyBoardAlive() {
        return filtered(fromPredicate(isAlive()), friendlyBoard());
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

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Minion> board() {
        return EntitySelector.merge(Arrays.asList(friendlyBoard(), enemyBoard()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Minion> boardBuffable() {
        return EntitySelector.merge(Arrays.asList(friendlyBoardBuffable(), enemyBoardBuffable()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Minion> boardAlive() {
        return EntitySelector.merge(Arrays.asList(friendlyBoardAlive(), enemyBoardAlive()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, TargetableCharacter> friends() {
        return EntitySelector.mergeToCommonBase(Arrays.asList(friendlyBoard(), friendlyHero()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, TargetableCharacter> friendsBuffable() {
        return EntitySelector.mergeToCommonBase(Arrays.asList(friendlyBoardBuffable(), friendlyHero()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, TargetableCharacter> friendsAlive() {
        return EntitySelector.mergeToCommonBase(Arrays.asList(friendlyBoardAlive(), friendlyHero()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, TargetableCharacter> enemies() {
        return fromOpponent(friends());
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, TargetableCharacter> enemiesBuffable() {
        return fromOpponent(friendsBuffable());
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, TargetableCharacter> enemiesAlive() {
        return fromOpponent(friendsAlive());
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, TargetableCharacter> allTargets() {
        return EntitySelector.mergeToCommonBase(Arrays.asList(friends(), enemies()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, TargetableCharacter> allTargetsBuffable() {
        return EntitySelector.mergeToCommonBase(Arrays.asList(friendsBuffable(), enemiesBuffable()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, TargetableCharacter> allTargetsAlive() {
        return EntitySelector.mergeToCommonBase(Arrays.asList(friendsAlive(), enemiesAlive()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Minion> allMinions() {
        return EntitySelector.merge(Arrays.asList(friendlyBoard(), enemyBoard()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Minion> allMinionsBuffable() {
        return EntitySelector.merge(Arrays.asList(friendlyBoardBuffable(), enemyBoardBuffable()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Minion> allMinionsAlive() {
        return EntitySelector.merge(Arrays.asList(friendlyBoardAlive(), enemyBoardAlive()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, TargetableCharacter> enemyTargets() {
        return EntitySelector.mergeToCommonBase(Arrays.asList(enemyBoard(), enemyHero()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, TargetableCharacter> enemyTargetsBuffable() {
        return EntitySelector.mergeToCommonBase(Arrays.asList(enemyBoardBuffable(), enemyHero()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, TargetableCharacter> enemyTargetsAlive() {
        return EntitySelector.mergeToCommonBase(Arrays.asList(enemyBoardAlive(), enemyHero()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, TargetableCharacter> friendlyTargets() {
        return EntitySelector.mergeToCommonBase(Arrays.asList(friendlyBoard(), friendlyHero()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, TargetableCharacter> friendlyTargetsBuffable() {
        return EntitySelector.mergeToCommonBase(Arrays.asList(friendlyBoardBuffable(), friendlyHero()));
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, TargetableCharacter> friendlyTargetsAlive() {
        return EntitySelector.mergeToCommonBase(Arrays.asList(friendlyBoardAlive(), friendlyHero()));
    }

    public static <Actor extends Minion> EntitySelector<Actor, Minion> neighbours() {
        TargetedEntitySelector<Actor, Minion, Minion> targetedSelector = TargetedEntitySelectors.targetsNeighbours();
        return (World world, Actor actor) -> {
            return targetedSelector.select(world, actor, actor);
        };
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Weapon> friendlyWeapon() {
        return (World world, Actor actor) -> {
            Weapon weapon = actor.getOwner().tryGetWeapon();
            return weapon != null ? Stream.of(weapon) : Stream.empty();
        };
    }

    public static <Actor extends PlayerProperty> EntitySelector<Actor, Weapon> enemyWeapon() {
        return (World world, Actor actor) -> {
            Weapon weapon = actor.getOwner().getOpponent().tryGetWeapon();
            return weapon != null ? Stream.of(weapon) : Stream.empty();
        };
    }

    private EntitySelectors() {
        throw new AssertionError();
    }
}
