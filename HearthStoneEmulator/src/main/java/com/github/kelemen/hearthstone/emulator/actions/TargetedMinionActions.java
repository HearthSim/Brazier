package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.BornEntity;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.jtrim.utils.ExceptionHelper;

public final class TargetedMinionActions {
    public static final TargetedMinionAction DAMAGE_TARGET_WITH_ATTACK = (Minion targeter, PlayTarget target) -> {
            TargetableCharacter character = target.getTarget();
            if (character == null) {
                return UndoAction.DO_NOTHING;
            }

            int damage = targeter.getAttackTool().getAttack();
            return ActionUtils.damageCharacter(targeter, damage, character);
    };

    public static TargetedMinionAction randomAction(
            @NamedArg("action") TargetedMinionAction[] actions) {
        TargetedMinionAction[] actionsCopy = actions.clone();
        ExceptionHelper.checkNotNullElements(actionsCopy, "actions");

        return (Minion targeter, PlayTarget target) -> {
            TargetedMinionAction action = ActionUtils.pickRandom(targeter.getWorld(), actionsCopy);
            return action.doAction(targeter, target);
        };
    }

    public static TargetedMinionAction damageTarget(
            @NamedArg("damage") int damage) {
        return damageTarget(damage, damage);
    }

    public static TargetedMinionAction damageTarget(
            @NamedArg("minDamage") int minDamage,
            @NamedArg("maxDamage") int maxDamage) {
        ExceptionHelper.checkArgumentInRange(maxDamage, minDamage, Integer.MAX_VALUE, "maxDamage");

        return (targeter, target) -> {
            TargetableCharacter character = target.getTarget();
            if (character == null) {
                return UndoAction.DO_NOTHING;
            }

            World world = targeter.getWorld();
            int damage = world.getRandomProvider().roll(minDamage, maxDamage);
            return ActionUtils.damageCharacter(targeter, damage, character);
        };
    }

    public static TargetedMinionAction forEnemyMinions(@NamedArg("action") TargetedMinionAction action) {
        return forEnemyMinions(WorldEventFilter.ANY, action);
    }

    public static TargetedMinionAction forOwnMinions(@NamedArg("action") TargetedMinionAction action) {
        return forOwnMinions(WorldEventFilter.ANY, action);
    }

    public static TargetedMinionAction forEnemyMinions(
            @NamedArg("filter") WorldEventFilter<? super Minion, ? super Minion> filter,
            @NamedArg("action") TargetedMinionAction action) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");
        ExceptionHelper.checkNotNullArgument(action, "action");
        return forMinions(action, (targeter, targets) -> {
            targeter.getOwner().getOpponent().getBoard().collectMinions(targets, (minion) -> {
                return minion.notScheduledToDestroy() && filter.applies(targeter.getWorld(), targeter, minion);
            });
        });
    }

    public static TargetedMinionAction forOwnMinions(
            @NamedArg("filter") WorldEventFilter<? super Minion, ? super Minion> filter,
            @NamedArg("action") TargetedMinionAction action) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");
        ExceptionHelper.checkNotNullArgument(action, "action");
        return forMinions(action, (targeter, targets) -> {
            targeter.getOwner().getBoard().collectMinions(targets, (minion) -> {
                return minion.notScheduledToDestroy() && filter.applies(targeter.getWorld(), targeter, minion);
            });
        });
    }

    private static TargetedMinionAction forMinions(
            TargetedMinionAction action,
            BiConsumer<Minion, List<Minion>> minionCollector) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        ExceptionHelper.checkNotNullArgument(minionCollector, "minionCollector");

        return (Minion targeter, PlayTarget playTarget) -> {
            List<Minion> targets = new ArrayList<>();
            Player castingPlayer = playTarget.getCastingPlayer();
            minionCollector.accept(targeter, targets);

            if (targets.isEmpty()) {
                return UndoAction.DO_NOTHING;
            }

            BornEntity.sortEntities(targets);

            UndoBuilder result = new UndoBuilder(targets.size());
            for (Minion minion: targets) {
                result.addUndo(action.doAction(targeter, new PlayTarget(castingPlayer, minion)));
            }
            return result;
        };
    }

    private TargetedMinionActions() {
        throw new AssertionError();
    }
}
