package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.Damage;
import com.github.kelemen.hearthstone.emulator.DamageSource;
import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.Keywords;
import com.github.kelemen.hearthstone.emulator.LabeledEntity;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.RandomProvider;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.UndoableIntResult;
import com.github.kelemen.hearthstone.emulator.UndoableRegistry;
import com.github.kelemen.hearthstone.emulator.UndoableResult;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.abilities.ActivatableAbility;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jtrim.utils.ExceptionHelper;

public final class ActionUtils {
    public static Function<World, MinionDescr> randomMinionProvider(Keyword[] keywords) {
        Function<World, List<CardDescr>> cardProvider = minionCardProvider(keywords);
        return (world) -> {
            List<CardDescr> cards = cardProvider.apply(world);
            CardDescr card = ActionUtils.pickRandom(world, cards);
            return card != null ? card.getMinion() : null;
        };
    }

    private static Function<World, List<CardDescr>> minionCardProvider(Keyword[] keywords) {
        ExceptionHelper.checkNotNullElements(keywords, "keywords");

        Keyword[] cardKeywords = new Keyword[keywords.length + 1];
        cardKeywords[0] = Keywords.MINION;
        System.arraycopy(keywords, 0, cardKeywords, 1, keywords.length);

        AtomicReference<List<CardDescr>> cache = new AtomicReference<>(null);
        return (world) -> {
            List<CardDescr> result = cache.get();
            if (result == null) {
                result = world.getDb().getCardDb().getByKeywords(cardKeywords);
                if (!cache.compareAndSet(null, result)) {
                    result = cache.get();
                }
            }
            return result;
        };
    }

    public static UndoAction attackWithHero(Player damageSource, boolean spell, int damage, TargetableCharacter target) {
        return attackWithHero(damageSource.getHero(), spell, damage, target);
    }

    public static UndoAction attackWithHero(Hero damageSource, boolean spell, int damage, TargetableCharacter target) {
        if (!spell) {
            return ActionUtils.damageCharacter(damageSource, damage, target);
        }

        Damage appliedDamage = damageSource.getOwner().getSpellDamage(damage);
        return target.damage(appliedDamage);
    }

    public static UndoAction damageCharacter(DamageSource damageSource, int damage, TargetableCharacter target) {
        UndoableResult<Damage> damageRef = damageSource.createDamage(damage);
        UndoableIntResult damageUndo = target.damage(damageRef.getResult());
        return () -> {
            damageUndo.undo();
            damageRef.undo();
        };
    }

    public static TargetableCharacter rollTarget(World world) {
        List<TargetableCharacter> result = new ArrayList<>(2 * (Player.MAX_BOARD_SIZE + 1));
        collectTargets(world.getPlayer1(), result);
        collectTargets(world.getPlayer2(), result);

        int roll = world.getRandomProvider().roll(result.size());
        return result.get(roll);
    }

    public static TargetableCharacter rollTarget(World world, Predicate<? super TargetableCharacter> filter) {
        List<TargetableCharacter> result = new ArrayList<>(2 * (Player.MAX_BOARD_SIZE + 1));
        collectTargets(world.getPlayer1(), result, filter);
        collectTargets(world.getPlayer2(), result, filter);

        int roll = world.getRandomProvider().roll(result.size());
        return result.get(roll);
    }

    public static TargetableCharacter rollAlivePlayerTarget(World world, Player player) {
        return rollPlayerTarget(world, player, (target) -> !target.isDead());
    }

    public static TargetableCharacter rollPlayerTarget(World world, Player player) {
        return rollPlayerTarget(world, player, (target) -> true);
    }

    public static TargetableCharacter rollPlayerTarget(
            World world,
            Player player,
            Predicate<? super TargetableCharacter> filter) {

        List<TargetableCharacter> result = new ArrayList<>(Player.MAX_BOARD_SIZE + 1);
        collectTargets(player, result, filter);

        int roll = world.getRandomProvider().roll(result.size());
        return result.get(roll);
    }

    public static void collectAliveTargets(Player player, List<TargetableCharacter> result) {
        collectTargets(player, result, (target) -> !target.isDead());
    }

    public static void collectTargets(Player player, List<? super TargetableCharacter> result) {
        collectTargets(player, result, (target) -> true);
    }

    public static void collectAliveTargets(
            Player player,
            List<? super TargetableCharacter> result,
            Predicate<? super TargetableCharacter> filter) {
        collectTargets(player, result, (target) -> !target.isDead() && filter.test(target));
    }

    public static void collectTargets(
            World world,
            List<? super TargetableCharacter> result,
            Predicate<? super TargetableCharacter> filter) {
        collectTargets(world.getPlayer1(), result, filter);
        collectTargets(world.getPlayer2(), result, filter);
    }

    public static void collectTargets(
            Player player,
            List<? super TargetableCharacter> result,
            Predicate<? super TargetableCharacter> filter) {
        ExceptionHelper.checkNotNullArgument(player, "player");
        ExceptionHelper.checkNotNullArgument(result, "result");
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        if (filter.test(player.getHero())) {
            result.add(player.getHero());
        }
        player.getBoard().collectMinions(result, (minion) -> !minion.isScheduledToDestroy() && filter.test(minion));
    }

    public static UndoableResult<Card> pollDeckForCard(
            Player player,
            Predicate<? super Card> cardFilter) {
        return player.getBoard().getDeck().tryDrawRandom(player.getWorld().getRandomProvider(), cardFilter);
    }

    public static <E extends LabeledEntity> Predicate<E> includedKeywordsFilter(Keyword... includedKeywords) {
        if (includedKeywords.length == 0) {
            return (arg) -> true;
        }

        Keyword[] includedKeywordsCopy = includedKeywords.clone();
        ExceptionHelper.checkNotNullElements(includedKeywordsCopy, "includedKeywords");

        return (entity) -> {
            for (Keyword keyword: includedKeywordsCopy) {
                if (!entity.getKeywords().contains(keyword)) {
                    return false;
                }
            }
            return true;
        };
    }

    public static <E extends LabeledEntity> Predicate<E> excludedKeywordsFilter(Keyword... excludedKeywords) {
        Keyword[] excludedKeywordsCopy = excludedKeywords.clone();
        ExceptionHelper.checkNotNullElements(excludedKeywordsCopy, "excludedKeywords");

        return (entity) -> {
            Set<Keyword> minionKeywords = entity.getKeywords();
            for (Keyword keyword: excludedKeywordsCopy) {
                if (minionKeywords.contains(keyword)) {
                    return false;
                }
            }
            return true;
        };
    }

    public static Minion rollAliveMinionTarget(World world, Predicate<? super Minion> minionFilter) {
        ExceptionHelper.checkNotNullArgument(world, "world");
        ExceptionHelper.checkNotNullArgument(minionFilter, "minionFilter");

        List<Minion> result = new ArrayList<>(2 * Player.MAX_BOARD_SIZE);

        Predicate<Minion> includeMinion = (minion) -> !minion.isDead();
        includeMinion = includeMinion.and(minionFilter);

        world.getPlayer1().getBoard().collectMinions(result, includeMinion);
        world.getPlayer2().getBoard().collectMinions(result, includeMinion);

        if (result.isEmpty()) {
            return null;
        }

        int roll = world.getRandomProvider().roll(result.size());
        return result.get(roll);
    }

    public static <T> T pickRandom(World world, List<? extends T> list) {
        int size = list.size();
        if (size == 0) {
            return null;
        }

        int index = world.getRandomProvider().roll(size);
        return list.get(index);
    }

    public static <T> T pickRandom(World world, T[] list) {
        if (list.length == 0) {
            return null;
        }

        int index = world.getRandomProvider().roll(list.length);
        return list[index];
    }

    public static <T> List<T> pickMultipleRandom(World world, int count, List<? extends T> list) {
        int size = list.size();
        if (size == 0 || count <= 0) {
            return Collections.emptyList();
        }
        if (size == 1) {
            return Collections.singletonList(list.get(0));
        }

        RandomProvider rng = world.getRandomProvider();
        if (count == 1) {
            return Collections.singletonList(list.get(rng.roll(size)));
        }

        int[] indexes = new int[size];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
        }

        List<T> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int indexOfIndex = rng.roll(indexes.length - i);
            int choice = indexes[indexOfIndex];
            indexes[indexOfIndex] = indexes[indexes.length - i - 1];

            result.add(list.get(choice));
        }
        return result;
    }

    public static UndoAction doOnEndOfTurn(World world, UndoableAction action) {
        WorldActionEvents<Player> listeners = world.getEvents().turnEndsListeners();

        AtomicReference<UndoableUnregisterRef> listenerRefRef = new AtomicReference<>();
        UndoableUnregisterRef listenerRef = listeners.addAction((World eventWorld, Player player) -> {
            UndoAction unregisterUndo = listenerRefRef.get().unregister();
            UndoAction actionUndo = action.doAction();

            return () -> {
                actionUndo.undo();
                unregisterUndo.undo();
            };
        });
        listenerRefRef.set(listenerRef);

        return listenerRef;
    }

    public static <Self> ActivatableAbility<Self> toSingleTurnAbility(
            World world,
            ActivatableAbility<Self> ability) {
        ExceptionHelper.checkNotNullArgument(world, "world");
        ExceptionHelper.checkNotNullArgument(ability, "ability");

        return (Self self) -> {
            UndoableUnregisterRef ref = ability.activate(self);
            UndoAction unregUndo = unregisterAfterTurnEnds(world, ref);

            return new UndoableUnregisterRef() {
                @Override
                public UndoAction unregister() {
                    return ref.unregister();
                }

                @Override
                public void undo() {
                    unregUndo.undo();
                    ref.undo();
                }
            };
        };
    }

    private static UndoAction unregisterAfterTurnEnds(World world, UndoableUnregisterRef ref) {
        WorldActionEvents<Player> turnEndsListeners = world.getEvents().turnEndsListeners();
        return turnEndsListeners.addAction((World taskWorld, Player object) -> ref.unregister());
    }

    public static UndoAction doTemporary(World world, UndoableRegistry buffRegisterAction) {
        UndoableUnregisterRef buffRef = buffRegisterAction.register();
        UndoAction unregUndo = unregisterAfterTurnEnds(world, buffRef);

        return () -> {
            unregUndo.undo();
            buffRef.undo();
        };
    }

    public static <Self> ActivatableAbility<Self> toUntilTurnStartsAbility(
            World world,
            PlayerProperty turnOwner,
            ActivatableAbility<Self> ability) {
        ExceptionHelper.checkNotNullArgument(world, "world");
        ExceptionHelper.checkNotNullArgument(turnOwner, "turnOwner");
        ExceptionHelper.checkNotNullArgument(ability, "ability");

        return (Self self) -> {
            UndoableUnregisterRef ref = ability.activate(self);
            UndoAction unregUndo = unregisterOnNextTurn(world, turnOwner, ref);

            return new UndoableUnregisterRef() {
                @Override
                public UndoAction unregister() {
                    return ref.unregister();
                }

                @Override
                public void undo() {
                    unregUndo.undo();
                    ref.undo();
                }
            };
        };
    }

    private static UndoAction unregisterOnNextTurn(
            World world,
            PlayerProperty turnOwner,
            UndoableUnregisterRef ref) {
        WorldActionEvents<Player> turnStartsListeners = world.getEvents().turnStartsListeners();
        return turnStartsListeners.addAction((World taskWorld, Player actionPlayer) -> {
            if (actionPlayer == turnOwner.getOwner()) {
                return ref.unregister();
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        });
    }

    public static UndoAction doUntilNewTurnStart(World world, PlayerProperty turnOwner, UndoableRegistry buffRegisterAction) {
        UndoableUnregisterRef buffRef = buffRegisterAction.register();
        UndoAction unregUndo = unregisterOnNextTurn(world, turnOwner, buffRef);

        return () -> {
            unregUndo.undo();
            buffRef.undo();
        };
    }

    private ActionUtils() {
        throw new AssertionError();
    }
}
