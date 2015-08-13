package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionProvider;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.jtrim.utils.ExceptionHelper;

import static com.github.kelemen.hearthstone.emulator.actions.BasicFilters.validMisdirectTarget;


public final class AttackEventActions {
     public static final WorldEventAction<PlayerProperty, AttackRequest> MISSDIRECT = (world, self, eventSource) -> {
         Predicate<TargetableCharacter> filter = validMisdirectTarget(eventSource);
         List<TargetableCharacter> targets = new ArrayList<>();
         ActionUtils.collectAliveTargets(world.getPlayer1(), targets, filter);
         ActionUtils.collectAliveTargets(world.getPlayer2(), targets, filter);

         TargetableCharacter selected = ActionUtils.pickRandom(world, targets);
         if (selected == null) {
             return UndoAction.DO_NOTHING;
         }

         return eventSource.replaceDefender(selected);
     };

    public static final WorldEventAction<PlayerProperty, AttackRequest> MISS_TARGET_SOMETIMES
            = missTargetSometimes(1, 2);

    public static WorldEventAction<PlayerProperty, AttackRequest> missTargetSometimes(
            @NamedArg("missCount") int missCount,
            @NamedArg("attackCount") int attackCount) {

        return (World world, PlayerProperty self, AttackRequest eventSource) -> {
            TargetableCharacter defender = eventSource.getDefender();
            if (defender == null) {
                return UndoAction.DO_NOTHING;
            }

            int roll = world.getRandomProvider().roll(attackCount);
            if (roll >=  missCount) {
                return UndoAction.DO_NOTHING;
            }

            List<TargetableCharacter> targets = new ArrayList<>(Player.MAX_BOARD_SIZE);
            ActionUtils.collectAliveTargets(defender.getOwner(), targets, (target) -> target != defender);
            TargetableCharacter newTarget = ActionUtils.pickRandom(world, targets);
            if (newTarget == null) {
                return UndoAction.DO_NOTHING;
            }

            return eventSource.replaceDefender(newTarget);
        };
    }

    public static WorldEventAction<PlayerProperty, AttackRequest> doForAttacker(
            @NamedArg("action") CharacterTargetedAction action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, PlayerProperty self, AttackRequest eventSource) -> {
            return action.alterWorld(world, eventSource.getAttacker());
        };
    }

    public static WorldEventAction<PlayerProperty, AttackRequest> returnAttacker(
            @NamedArg("costReduction") int costReduction) {

        CharacterTargetedAction returnMinionAction
                = ActorlessTargetedActions.applyToMinionTarget(MinionActions.returnMinion(costReduction));
        return (world, self, eventSource) -> {
            return returnMinionAction.alterWorld(world, eventSource.getAttacker());
        };
    }

    public static WorldEventAction<PlayerProperty, AttackRequest> summonNewTargetForAttack(
            @NamedArg("minion") MinionProvider minion) {
        return (world, self, eventSource) -> {
            Player targetPlayer = self.getOwner();
            if (targetPlayer.getBoard().isFull()) {
                return UndoAction.DO_NOTHING;
            }

            Minion summonedMinion = new Minion(targetPlayer, minion.getMinion());
            UndoAction summonUndo = targetPlayer.summonMinion(summonedMinion);
            UndoAction retargetUndo = eventSource.replaceDefender(summonedMinion);
            return () -> {
                retargetUndo.undo();
                summonUndo.undo();
            };
        };
    }

    private AttackEventActions() {
        throw new AssertionError();
    }
}
