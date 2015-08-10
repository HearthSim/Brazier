package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.World;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface PlayerAction extends WorldObjectAction<Player> {
    @Override
    public UndoAction alterWorld(World world, Player player);

    public default TargetedAction toTargetedAction() {
        return (world, target) -> alterWorld(world, target.getCastingPlayer());
    }

    public default BattleCryTargetedAction toBattleCryTargetedAction() {
        return (world, target) -> alterWorld(world, target.getCastingPlayer());
    }

    public static PlayerAction merge(Collection<? extends PlayerAction> actions) {
        List<PlayerAction> actionsCopy = new ArrayList<>(actions);
        ExceptionHelper.checkNotNullElements(actionsCopy, "actions");

        int count = actionsCopy.size();
        if (count == 0) {
            return (world, arg) -> UndoAction.DO_NOTHING;
        }
        if (count == 1) {
            return actionsCopy.get(0);
        }

        return (World world, Player player) -> {
            UndoBuilder result = new UndoBuilder(actionsCopy.size());
            for (PlayerAction action: actionsCopy) {
                result.addUndo(action.alterWorld(world, player));
            }
            return result;
        };
    }
}
