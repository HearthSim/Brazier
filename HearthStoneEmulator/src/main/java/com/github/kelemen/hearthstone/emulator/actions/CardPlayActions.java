package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionProvider;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.Arrays;
import org.jtrim.utils.ExceptionHelper;

public final class CardPlayActions {
    public static CardPlayAction doAtomic(
            @NamedArg("action") CardPlayAction action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, CardPlayArg arg) -> {
            return world.getEvents().doAtomic(() -> action.alterWorld(world, arg));
        };
    }

    public static CardPlayAction combine(
            @NamedArg("actions") CardPlayAction[] actions) {
        return CardPlayAction.mergeActions(Arrays.asList(actions));
    }

    public static CardPlayAction doIf(
            @NamedArg("condition") PlayActionRequirement condition,
            @NamedArg("action") CardPlayAction action) {
        ExceptionHelper.checkNotNullArgument(condition, "condition");
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, CardPlayArg arg) -> {
            if (condition.meetsRequirement(arg.getTarget().getCastingPlayer())) {
                return action.alterWorld(world, arg);
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    public static CardPlayAction summonMinionRight(@NamedArg("minion") MinionProvider minion) {
        return (World world, CardPlayArg arg) -> {
            Minion selfMinion = arg.getCard().getMinion();
            if (selfMinion != null) {
                return selfMinion.getLocationRef().summonRight(minion.getMinion());
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    public static CardPlayAction dealMinionDamage(@NamedArg("damage") int damage) {
        return (world, arg) -> {
            Minion minion = arg.getCard().getMinion();
            if (minion == null) {
                return UndoAction.DO_NOTHING;
            }

            TargetableCharacter character = arg.getTarget().getTarget();
            if (character == null) {
                return UndoAction.DO_NOTHING;
            }

            return ActionUtils.damageCharacter(minion, damage, character);
        };
    }

    private CardPlayActions() {
        throw new AssertionError();
    }
}
