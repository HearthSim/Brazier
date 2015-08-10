package com.github.kelemen.hearthstone.emulator.abilities;

import com.github.kelemen.hearthstone.emulator.World;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface AuraFilter<Source, Target> {
    public static final AuraFilter<Object, Object> ANY = (world, source, target) -> true;

    public boolean isApplicable(World world, Source source, Target target);

    public static <Source, Target> AuraFilter<Source, Target> and(
            AuraFilter<? super Source, ? super Target> filter1,
            AuraFilter<? super Source, ? super Target> filter2) {
        return (World world, Source source, Target target) -> {
            return filter1.isApplicable(world, source, target) && filter2.isApplicable(world, source, target);
        };
    }

    public static <Self, T> AuraFilter<? super Self, ? super T> merge(
            Collection<? extends AuraFilter<? super Self, ? super T>> filters) {

        int filterCount = filters.size();
        if (filterCount == 0) {
            return AuraFilter.ANY;
        }
        if (filterCount == 1) {
            return filters.iterator().next();
        }

        List<AuraFilter<? super Self, ? super T>> filtersCopy = new ArrayList<>(filters);
        ExceptionHelper.checkNotNullElements(filtersCopy, "filters");

        return (World world, Self owner, T eventSource) -> {
            for (AuraFilter<? super Self, ? super T> filter: filtersCopy) {
                if (!filter.isApplicable(world, owner, eventSource)) {
                    return false;
                }
            }
            return true;
        };
    }
}
