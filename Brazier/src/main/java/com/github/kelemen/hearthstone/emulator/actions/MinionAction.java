package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface MinionAction extends WorldObjectAction<Minion> {
    @Override
    public UndoAction alterWorld(World world, Minion minion);

    public default BattleCryTargetedAction toBattleCryTargetedAction() {
        return (World world, BattleCryArg arg) -> alterWorld(world, arg.getSource());
    }

    public default CharacterTargetedAction toCharacterTargetedAction() {
        return (World world, TargetableCharacter target) -> {
            return target instanceof Minion
                    ? alterWorld(world, (Minion)target)
                    : UndoAction.DO_NOTHING;
        };
    }

    public static MinionAction merge(Collection<? extends MinionAction> actions) {
        List<MinionAction> actionsCopy = new ArrayList<>(actions);
        ExceptionHelper.checkNotNullElements(actionsCopy, "actions");

        int count = actionsCopy.size();
        if (count == 0) {
            return (world, object) -> UndoAction.DO_NOTHING;
        }
        if (count == 1) {
            return actionsCopy.get(0);
        }

        return (World world, Minion self) -> {
            UndoBuilder result = new UndoBuilder(actionsCopy.size());
            for (MinionAction action: actionsCopy) {
                result.addUndo(action.alterWorld(world, self));
            }
            return result;
        };
    }
}