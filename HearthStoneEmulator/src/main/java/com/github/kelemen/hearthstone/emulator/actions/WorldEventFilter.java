package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.World;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface WorldEventFilter<Self, T> {
    public static final WorldEventFilter<Object, Object> ANY = (world, source, target) -> true;

    public boolean applies(World world, Self owner, T eventSource);

    public static <Self, T> WorldEventFilter<? super Self, ? super T> merge(
            Collection<? extends WorldEventFilter<? super Self, ? super T>> filters) {

        int filterCount = filters.size();
        if (filterCount == 0) {
            return BasicFilters.ANY;
        }
        if (filterCount == 1) {
            return filters.iterator().next();
        }

        List<WorldEventFilter<? super Self, ? super T>> filtersCopy = new ArrayList<>(filters);
        ExceptionHelper.checkNotNullElements(filtersCopy, "filters");

        return (World world, Self owner, T eventSource) -> {
            for (WorldEventFilter<? super Self, ? super T> filter: filtersCopy) {
                if (!filter.applies(world, owner, eventSource)) {
                    return false;
                }
            }
            return true;
        };
    }
}
