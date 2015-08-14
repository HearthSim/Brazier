package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface TargetedAction<Actor, Target> {
    public UndoAction alterWorld(World world, Actor actor, Target target);

    public static <Actor, Target> TargetedAction<Actor, Target> merge(
            Collection<? extends TargetedAction<Actor, Target>> actions) {
        List<TargetedAction<Actor, Target>> actionsCopy = new ArrayList<>(actions);
        ExceptionHelper.checkNotNullElements(actionsCopy, "actions");

        int count = actionsCopy.size();
        if (count == 0) {
            return (world, actor, target) -> UndoAction.DO_NOTHING;
        }
        if (count == 1) {
            return actionsCopy.get(0);
        }

        return (World world, Actor actor, Target target) -> {
            UndoBuilder result = new UndoBuilder(actionsCopy.size());
            for (TargetedAction<Actor, Target> action: actionsCopy) {
                result.addUndo(action.alterWorld(world, actor, target));
            }
            return result;
        };
    }
}
