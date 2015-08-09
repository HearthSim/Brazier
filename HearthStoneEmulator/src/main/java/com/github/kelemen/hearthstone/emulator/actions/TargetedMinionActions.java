package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
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

    private TargetedMinionActions() {
        throw new AssertionError();
    }
}
