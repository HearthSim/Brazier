package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.PlayerProperty;
import com.github.kelemen.brazier.TargetableCharacter;
import com.github.kelemen.brazier.events.WorldEventAction;
import com.github.kelemen.brazier.minions.Minion;
import com.github.kelemen.brazier.minions.MinionProvider;
import com.github.kelemen.brazier.parsing.NamedArg;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.github.kelemen.brazier.actions.BasicFilters.validMisdirectTarget;


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
