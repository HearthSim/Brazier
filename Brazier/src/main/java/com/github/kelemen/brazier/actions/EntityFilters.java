package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.Keywords;
import com.github.kelemen.hearthstone.emulator.LabeledEntity;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.ActionUtils;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
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

    public static Predicate<Minion> buffableMinion() {
        return (minion) -> !minion.isScheduledToDestroy();
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

    private EntityFilters() {
        throw new AssertionError();
    }
}
