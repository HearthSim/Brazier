package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.World;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface BattleCryTargetedAction extends WorldObjectAction<BattleCryArg> {
    @Override
    public UndoAction alterWorld(World world, BattleCryArg arg);

    public static BattleCryTargetedAction merge(Collection<? extends BattleCryTargetedAction> actions) {
        List<BattleCryTargetedAction> actionsCopy = new ArrayList<>(actions);
        ExceptionHelper.checkNotNullElements(actionsCopy, "actions");

        int count = actionsCopy.size();
        if (count == 0) {
            return (world, arg) -> UndoAction.DO_NOTHING;
        }
        if (count == 1) {
            return actionsCopy.get(0);
        }

        return (World world, BattleCryArg arg) -> {
            UndoBuilder result = new UndoBuilder(actionsCopy.size());
            for (BattleCryTargetedAction action: actionsCopy) {
                result.addUndo(action.alterWorld(world, arg));
            }
            return result;
        };
    }
}
