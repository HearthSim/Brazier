package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.DamageSource;
import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.MultiTargeter;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.function.Predicate;

public final class DamageActions {
    public static DamageAction damageRandomEnemyMinion(@NamedArg("damage") int damage) {
        return (World world, DamageSource damageSource) -> {
            Minion target = ActionUtils.rollAliveMinionTarget(world, (minion) -> {
                return minion.getOwner() != damageSource.getOwner();
            });
            if (target == null) {
                return UndoAction.DO_NOTHING;
            }
            return ActionUtils.damageCharacter(damageSource, damage, target);
        };
    }

    public static DamageAction damageRandomEnemyTarget(@NamedArg("damage") int damage) {
        return (World world, DamageSource damageSource) -> {
            Player opponent = damageSource.getOwner().getOpponent();
            TargetableCharacter target = ActionUtils.rollAlivePlayerTarget(world, opponent);
            if (target == null) {
                return UndoAction.DO_NOTHING;
            }
            return ActionUtils.damageCharacter(damageSource, damage, target);
        };
    }

    public static DamageAction damageOpponentHero(@NamedArg("damage") int damage) {
        return (World world, DamageSource damageSource) -> {
            Hero hero = damageSource.getOwner().getOpponent().getHero();
            return ActionUtils.damageCharacter(damageSource, damage, hero);
        };
    }

    public static DamageAction damageOwnHero(@NamedArg("damage") int damage) {
        return (World world, DamageSource damageSource) -> {
            Hero hero = damageSource.getOwner().getHero();
            return ActionUtils.damageCharacter(damageSource, damage, hero);
        };
    }

    public static DamageAction dealDamageToAll(
            @NamedArg("minions") boolean minions,
            @NamedArg("heroes") boolean heroes,
            @NamedArg("atomic") boolean atomic,
            @NamedArg("damage") int damage) {

        return dealDamageTo(true, true, minions, heroes, atomic, damage);
    }

    public static DamageAction dealDamageToEnemyTargets(@NamedArg("damage") int damage) {
        return dealDamageTo(true, false, true, true, damage);
    }

    public static DamageAction dealDamageToEnemyMinions(@NamedArg("damage") int damage) {
        return dealDamageTo(true, false, true, false, damage);
    }

    public static DamageAction dealDamageToOwnMinions(@NamedArg("damage") int damage) {
        return dealDamageTo(false, true, true, false, damage);
    }

    public static DamageAction dealDamageToOwnTargets(@NamedArg("damage") int damage) {
        return dealDamageTo(false, true, true, true, damage);
    }

    public static DamageAction dealDamageToAllMinions(@NamedArg("damage") int damage) {
        return dealDamageTo(true, true, true, false, damage);
    }

    public static DamageAction dealDamageToAllTargets(@NamedArg("damage") int damage) {
        return dealDamageTo(true, true, true, true, damage);
    }

    private static DamageAction dealDamageTo(
            boolean enemy,
            boolean self,
            boolean minions,
            boolean heroes,
            int damage) {
        return dealDamageTo(enemy, self, minions, heroes, damage < 0, damage);
    }

    private static DamageAction dealDamageTo(
            boolean enemy,
            boolean self,
            boolean minions,
            boolean heroes,
            boolean atomic,
            int damage) {
        return dealDamageTo(enemy, self, minions, heroes, atomic, (target) -> true, damage);
    }

    private static DamageAction dealDamageTo(
            boolean enemy,
            boolean self,
            boolean minions,
            boolean heroes,
            boolean atomic,
            Predicate<? super TargetableCharacter> filter,
            int damage) {

        MultiTargeter.Builder targeterBuilder = new MultiTargeter.Builder();
        targeterBuilder.setEnemy(enemy);
        targeterBuilder.setSelf(self);
        targeterBuilder.setMinions(minions);
        targeterBuilder.setHeroes(heroes);
        targeterBuilder.setAtomic(atomic);
        targeterBuilder.setCustomFilter(filter);

        MultiTargeter targeter = targeterBuilder.create();

        return (World world, DamageSource damageSource) -> {
            return targeter.damageTargets(damageSource.getOwner(), damageSource, damage);
        };
    }

    public static DamageAction dealDamageToOthers(
            @NamedArg("damage") int damage) {

        return (World world, DamageSource damageSource) -> {
            MultiTargeter.Builder targeterBuilder = new MultiTargeter.Builder();
            targeterBuilder.setEnemy(true);
            targeterBuilder.setSelf(true);
            targeterBuilder.setMinions(true);
            targeterBuilder.setHeroes(true);
            targeterBuilder.setCustomFilter((target) -> target != damageSource);

            MultiTargeter targeter = targeterBuilder.create();

            return targeter.damageTargets(damageSource.getOwner(), damageSource, damage);
        };
    }

    private DamageActions() {
        throw new AssertionError();
    }
}
