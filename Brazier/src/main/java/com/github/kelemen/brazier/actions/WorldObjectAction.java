package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.World;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface WorldObjectAction<T> {
    public UndoAction alterWorld(World world, T object);

    public default WorldAction toWorldAction(T object) {
        return (world) -> alterWorld(world, object);
    }

    public static <T> WorldObjectAction<T> merge(Collection<? extends WorldObjectAction<T>> actions) {
        List<WorldObjectAction<T>> actionsCopy = new ArrayList<>(actions);
        ExceptionHelper.checkNotNullElements(actionsCopy, "actions");

        int count = actionsCopy.size();
        if (count == 0) {
            return (world, object) -> UndoAction.DO_NOTHING;
        }
        if (count == 1) {
            return actionsCopy.get(0);
        }

        return (World world, T self) -> {
            UndoBuilder result = new UndoBuilder(actionsCopy.size());
            for (WorldObjectAction<T> action: actionsCopy) {
                result.addUndo(action.alterWorld(world, self));
            }
            return result;
        };
    }
}
