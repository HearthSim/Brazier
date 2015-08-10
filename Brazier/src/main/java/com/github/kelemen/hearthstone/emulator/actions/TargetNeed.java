package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.PlayerId;
import com.github.kelemen.hearthstone.emulator.PlayerPredicate;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import java.util.Objects;
import org.jtrim.utils.ExceptionHelper;

public final class TargetNeed {
    public static final TargetNeed NO_NEED = new TargetNeed(TargetNeed::allowNone, TargetNeed::allowNone, false, "NO-NEEDS");

    public static final TargetNeed ALL_HEROES = new TargetNeed(TargetNeed::allowAll, TargetNeed::allowNone, "ALL-HEROES");
    public static final TargetNeed ALL_MINIONS = new TargetNeed(TargetNeed::allowNone, TargetNeed::allowAll, "ALL-MINIONS");
    public static final TargetNeed SELF_MINIONS = new TargetNeed(TargetNeed::allowNone, TargetNeed::allowSelf, "SELF-MINIONS");
    public static final TargetNeed ENEMY_MINIONS = new TargetNeed(TargetNeed::allowNone, TargetNeed::allowEnemy, "ENEMY-MINIONS");
    public static final TargetNeed ALL_TARGETS = new TargetNeed(TargetNeed::allowAll, TargetNeed::allowAll, "ALL-TARGETS");
    public static final TargetNeed SELF_TARGETS = new TargetNeed(TargetNeed::allowSelf, TargetNeed::allowSelf, "SELF-TARGETS");
    public static final TargetNeed ENEMY_TARGETS = new TargetNeed(TargetNeed::allowEnemy, TargetNeed::allowEnemy, "ENEMY-TARGETS");

    private final boolean hasTarget;
    private final PlayerPredicate<? super Hero> allowHeroCondition;
    private final PlayerPredicate<? super Minion> allowMinionCondition;
    private final String name;

    public TargetNeed() {
        this(TargetNeed::allowNone, TargetNeed::allowNone, false, null);
    }

    public TargetNeed(PlayerPredicate<? super Hero> allowHeroCondition,
            PlayerPredicate<? super Minion> allowMinionCondition) {
        this(allowHeroCondition, allowMinionCondition, true, null);
    }

    private TargetNeed(PlayerPredicate<? super Hero> allowHeroCondition,
            PlayerPredicate<? super Minion> allowMinionCondition,
            String name) {
        this(allowHeroCondition, allowMinionCondition, true, name);
    }

    private TargetNeed(PlayerPredicate<? super Hero> allowHeroCondition,
            PlayerPredicate<? super Minion> allowMinionCondition,
            boolean hasTarget,
            String name) {
        ExceptionHelper.checkNotNullArgument(allowHeroCondition, "allowHeroCondition");
        ExceptionHelper.checkNotNullArgument(allowMinionCondition, "allowMinionCondition");

        this.allowHeroCondition = allowHeroCondition;
        this.allowMinionCondition = allowMinionCondition;
        this.hasTarget = hasTarget;
        this.name = name;
    }

    public TargetNeed combine(TargetNeed other) {
        if (!hasTarget) {
            return other;
        }
        if (!other.hasTarget) {
            return this;
        }

        PlayerPredicate<Hero> newHeroCond = (playerId, hero) -> {
            return allowHeroCondition.test(playerId, hero) && other.allowHeroCondition.test(playerId, hero);
        };
        PlayerPredicate<Minion> newMinionCond = (playerId, minion) -> {
            return allowMinionCondition.test(playerId, minion) && other.allowMinionCondition.test(playerId, minion);
        };

        String thisName = this.name;
        String otherName = other.name;
        String newName = thisName != null && otherName != null
                ? thisName + " and " + otherName
                : null;

        return new TargetNeed(newHeroCond, newMinionCond, newName);
    }

    public boolean hasTarget() {
        return hasTarget;
    }

    public PlayerPredicate<? super Hero> getAllowHeroCondition() {
        return allowHeroCondition;
    }

    public PlayerPredicate<? super Minion> getAllowMinionCondition() {
        return allowMinionCondition;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (this.hasTarget ? 1 : 0);
        hash = 31 * hash + Objects.hashCode(this.allowHeroCondition);
        hash = 31 * hash + Objects.hashCode(this.allowMinionCondition);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final TargetNeed other = (TargetNeed)obj;
        return this.hasTarget == other.hasTarget
                && Objects.equals(this.allowHeroCondition, other.allowHeroCondition)
                && Objects.equals(this.allowMinionCondition, other.allowMinionCondition);
    }

    @Override
    public String toString() {
        return name != null
                ? name
                : "TargetNeed{hasTarget=" + hasTarget + '}';
    }

    private static <T> boolean allowNone(PlayerId playerId, T arg) {
        return false;
    }

    private static <T> boolean allowAll(PlayerId playerId, T arg) {
        return true;
    }

    private static boolean allowEnemy(PlayerId playerId, Minion minion) {
        return !Objects.equals(playerId, minion.getOwner().getPlayerId());
    }

    private static boolean allowEnemy(PlayerId playerId, Hero hero) {
        return !Objects.equals(playerId, hero.getOwner().getPlayerId());
    }

    private static boolean allowSelf(PlayerId playerId, Minion minion) {
        return Objects.equals(playerId, minion.getOwner().getPlayerId());
    }

    private static boolean allowSelf(PlayerId playerId, Hero hero) {
        return Objects.equals(playerId, hero.getOwner().getPlayerId());
    }
}
