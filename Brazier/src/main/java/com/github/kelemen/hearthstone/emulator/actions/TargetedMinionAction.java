package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.minions.Minion;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface TargetedMinionAction {
    public UndoAction doAction(Minion targeter, PlayTarget target);

    public static TargetedMinionAction merge(Collection<? extends TargetedMinionAction> actions) {
        if (actions.size() == 1) {
            return actions.iterator().next();
        }

        List<TargetedMinionAction> actionsCopy = new ArrayList<>(actions);
        ExceptionHelper.checkNotNullElements(actionsCopy, "actions");

        return (Minion minion, PlayTarget target) -> {
            UndoBuilder result = new UndoBuilder(actionsCopy.size());
            for (TargetedMinionAction action: actionsCopy) {
                result.addUndo(action.doAction(minion, target));
            }
            return result;
        };
    }
}
