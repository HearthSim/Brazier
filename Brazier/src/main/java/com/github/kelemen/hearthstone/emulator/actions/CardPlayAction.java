package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.World;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface CardPlayAction extends WorldObjectAction<CardPlayArg> {
    public static final CardPlayAction DO_NOTHING = (world, arg) -> UndoAction.DO_NOTHING;

    @Override
    public UndoAction alterWorld(World world, CardPlayArg arg);

    public static CardPlayAction mergeActions(Collection<? extends CardPlayAction> actions) {
        if (actions.size() == 1) {
            return actions.iterator().next();
        }

        List<CardPlayAction> actionsCopy = new ArrayList<>(actions);
        ExceptionHelper.checkNotNullElements(actionsCopy, "actions");

        return (World world, CardPlayArg arg) -> {
            UndoBuilder result = new UndoBuilder(actionsCopy.size());
            for (CardPlayAction action: actionsCopy) {
                result.addUndo(action.alterWorld(world, arg));
            }
            return result;
        };
    }
}
