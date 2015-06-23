package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface TargetedAction extends WorldObjectAction<PlayTarget> {
    public static final TargetedAction DO_NOTHING = (world, target) -> UndoAction.DO_NOTHING;

    @Override
    public UndoAction alterWorld(World world, PlayTarget target);

    public default BattleCryTargetedAction toBattleCryTargetedAction() {
        return (World world, BattleCryArg arg) -> {
            return alterWorld(world, arg.getTarget());
        };
    }

    public default TargetedMinionAction toTargetedMinionAction() {
        return (Minion minion, PlayTarget target) -> {
            return alterWorld(minion.getWorld(), target);
        };
    }

    public default CardPlayAction toCardPlayAction() {
        return (World world, CardPlayArg arg) -> {
            return alterWorld(world, arg.getTarget());
        };
    }

    public static TargetedAction mergeActions(Collection<? extends TargetedAction> actions) {
        if (actions.size() == 1) {
            return actions.iterator().next();
        }

        List<TargetedAction> actionsCopy = new ArrayList<>(actions);
        ExceptionHelper.checkNotNullElements(actionsCopy, "actions");

        return (World world, PlayTarget target) -> {
            UndoBuilder result = new UndoBuilder(actionsCopy.size());
            for (TargetedAction action: actionsCopy) {
                result.addUndo(action.alterWorld(world, target));
            }
            return result;
        };
    }
}
