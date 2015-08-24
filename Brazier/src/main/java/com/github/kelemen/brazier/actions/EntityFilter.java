package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.World;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.jtrim.utils.ExceptionHelper;

public interface EntityFilter<Entity> {
    public Stream<? extends Entity> select(World world, Stream<? extends Entity> entities);

    public static <Entity> EntityFilter<Entity> merge(
            Collection<? extends EntityFilter<Entity>> filters) {
        List<EntityFilter<Entity>> filtersCopy = new ArrayList<>(filters);
        ExceptionHelper.checkNotNullElements(filtersCopy, "filters");

        int count = filtersCopy.size();
        if (count == 0) {
            return EntityFilters.empty();
        }
        if (count == 1) {
            return filtersCopy.get(0);
        }

        return (World world, Stream<? extends Entity> entities) -> {
            Stream<? extends Entity> currentTargets = entities;
            for (EntityFilter<Entity> filter: filtersCopy) {
                currentTargets = filter.select(world, currentTargets);
            }
            return currentTargets;
        };
    }
}
