package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.World;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface WorldEventAction<Self extends PlayerProperty, EventSource> {
    public static final WorldEventAction<PlayerProperty, Object> DO_NOTHING
            = (world, self, eventSource) -> UndoAction.DO_NOTHING;

    public UndoAction alterWorld(World world, Self self, EventSource eventSource);

    public default WorldEventAction<Self, EventSource> withFilter(WorldEventFilter<? super Self, ? super EventSource> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        return (World world, Self self, EventSource eventSource) -> {
            if (filter.applies(world, self, eventSource)) {
                return alterWorld(world, self, eventSource);
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    public static <Self extends PlayerProperty, EventSource> WorldEventAction<? super Self, ? super EventSource> merge(
            Collection<? extends WorldEventAction<? super Self, ? super EventSource>> actions) {

        int filterCount = actions.size();
        if (filterCount == 0) {
            return (world, self, eventSource) -> UndoAction.DO_NOTHING;
        }
        if (filterCount == 1) {
            return actions.iterator().next();
        }

        List<WorldEventAction<? super Self, ? super EventSource>> actionsCopy = new ArrayList<>(actions);
        ExceptionHelper.checkNotNullElements(actionsCopy, "actions");

        return (World world, Self owner, EventSource eventSource) -> {
            UndoBuilder result = new UndoBuilder(actionsCopy.size());
            for (WorldEventAction<? super Self, ? super EventSource> action: actionsCopy) {
                result.addUndo(action.alterWorld(world, owner, eventSource));
            }
            return result;
        };
    }
}
