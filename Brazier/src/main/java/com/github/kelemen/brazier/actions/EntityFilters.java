package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.Keyword;
import com.github.kelemen.brazier.Keywords;
import com.github.kelemen.brazier.LabeledEntity;
import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.PlayerProperty;
import com.github.kelemen.brazier.TargetableCharacter;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.minions.Minion;
import com.github.kelemen.brazier.minions.MinionId;
import com.github.kelemen.brazier.parsing.NamedArg;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jtrim.utils.ExceptionHelper;

public final class EntityFilters {

    public static <Entity> EntityFilter<Entity> empty() {
        return (world, entities) -> Stream.empty();
    }

    public static <Entity> EntityFilter<Entity> random() {
        return (World world, Stream<? extends Entity> entities) -> {
            List<Entity> elements = entities.collect(Collectors.<Entity>toList());
            Entity result = ActionUtils.pickRandom(world, elements);
            if (result == null) {
                return Stream.empty();
            }
            else {
                return Stream.of(result);
            }
        };
    }

    public static <Entity> EntityFilter<Entity> random(@NamedArg("count") int count) {
        ExceptionHelper.checkArgumentInRange(count, 0, Integer.MAX_VALUE, "count");
        if (count == 0) {
            return empty();
        }
        if (count == 1) {
            return random();
        }

        return (World world, Stream<? extends Entity> entities) -> {
            List<Entity> elements = entities.collect(Collectors.<Entity>toList());
            return ActionUtils.pickMultipleRandom(world, count, elements).stream();
        };
    }

    public static <Entity extends LabeledEntity> Predicate<Entity> withKeywords(
            @NamedArg("keywords") Keyword... keywords) {
        return ActionUtils.includedKeywordsFilter(keywords);
    }

    public static <Entity extends LabeledEntity> Predicate<Entity> withoutKeywords(
            @NamedArg("keywords") Keyword... keywords) {
        return ActionUtils.excludedKeywordsFilter(keywords);
    }

    public static <Entity extends TargetableCharacter> Predicate<Entity> isBeast() {
        return withKeywords(Keywords.RACE_BEAST);
    }

    public static <Entity extends TargetableCharacter> Predicate<Entity> isDemon() {
        return withKeywords(Keywords.RACE_DEMON);
    }

    public static <Entity extends TargetableCharacter> Predicate<Entity> isDragon() {
        return withKeywords(Keywords.RACE_DRAGON);
    }

    public static <Entity extends TargetableCharacter> Predicate<Entity> isMech() {
        return withKeywords(Keywords.RACE_MECH);
    }

    public static <Entity extends TargetableCharacter> Predicate<Entity> isMurloc() {
        return withKeywords(Keywords.RACE_MURLOC);
    }

    public static <Entity extends TargetableCharacter> Predicate<Entity> isPirate() {
        return withKeywords(Keywords.RACE_PIRATE);
    }

    public static <Entity extends TargetableCharacter> Predicate<Entity> isTotem() {
        return withKeywords(Keywords.RACE_TOTEM);
    }

    public static <Entity extends TargetableCharacter> Predicate<Entity> isDead() {
        return (target) -> target.isDead();
    }

    public static <Entity extends TargetableCharacter> Predicate<Entity> isAlive() {
        return (target) -> !target.isDead();
    }

    public static <Entity extends TargetableCharacter> Predicate<Entity> isDamaged() {
        return (target) -> target.isDamaged();
    }

    public static <Entity extends TargetableCharacter> Predicate<Entity> isUndamaged() {
        return (target) -> !target.isDamaged();
    }

    public static <Entity extends TargetableCharacter> Predicate<Entity> isFrozen() {
        return (target) -> target.getAttackTool().isFrozen();
    }

    public static <Entity extends Minion> Predicate<Entity> isDeathRattle() {
        return (target) -> target.getProperties().isDeathRattle();
    }

    public static Predicate<Minion> buffableMinion() {
        return (minion) -> !minion.isScheduledToDestroy();
    }

    public static <Entity extends Minion> Predicate<Entity> minionNameIs(@NamedArg("name") MinionId name) {
        ExceptionHelper.checkNotNullArgument(name, "name");
        return (target) -> name.equals(target.getBaseDescr().getId());
    }

    public static <Entity> Predicate<Entity> not(@NamedArg("filter") Predicate<? super Entity> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");
        return (arg) -> !filter.test(arg);
    }

    public static <Entity> EntityFilter<Entity> fromPredicate(Predicate<? super Entity> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        return (World world, Stream<? extends Entity> entities) -> {
            return entities.filter(filter);
        };
    }

    public static <Entity extends TargetableCharacter> Predicate<Entity> attackIsLess(@NamedArg("attack") int attack) {
        return (target) -> target.getAttackTool().getAttack() < attack;
    }

    public static <Entity extends PlayerProperty> Predicate<Entity> isMaxManaCrystals() {
        return (target) -> target.getOwner().getManaResource().getManaCrystals() >= Player.MAX_MANA;
    }

    public static <Entity extends PlayerProperty> Predicate<Entity> isEmptyHand() {
        return (target) -> target.getOwner().getHand().getCardCount() == 0;
    }

    private EntityFilters() {
        throw new AssertionError();
    }
}
