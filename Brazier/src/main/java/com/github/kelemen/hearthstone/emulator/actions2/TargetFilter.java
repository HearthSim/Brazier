package com.github.kelemen.hearthstone.emulator.actions2;

import com.github.kelemen.hearthstone.emulator.World;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.jtrim.utils.ExceptionHelper;

public interface TargetFilter<Actor, Target> extends EntitySelector<Actor, Stream<Target>, Target> {
    public static <Actor, Target> TargetFilter<Actor, Target> merge(
            Collection<? extends TargetFilter<Actor, Target>> filters) {
        List<TargetFilter<Actor, Target>> filtersCopy = new ArrayList<>(filters);
        ExceptionHelper.checkNotNullElements(filtersCopy, "filters");

        int count = filtersCopy.size();
        if (count == 0) {
            return TargetFilters.noTarget();
        }
        if (count == 1) {
            return filtersCopy.get(0);
        }

        return (World world, Actor actor, Stream<Target> target) -> {
            Stream<Target> currentTargets = target;
            for (TargetFilter<Actor, Target> filter: filtersCopy) {
                currentTargets = filter.select(world, actor, currentTargets);
            }
            return currentTargets;
        };
    }
}
