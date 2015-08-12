package com.github.kelemen.hearthstone.emulator.actions2;

import com.github.kelemen.hearthstone.emulator.World;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.jtrim.utils.ExceptionHelper;

public interface EntitySelector<Actor, Target, Selection> {
    public Stream<? extends Selection> select(World world, Actor actor, Target target);

    public static <Actor, Target, Selection> EntitySelector<Actor, Target, Selection> merge(
            Collection<? extends EntitySelector<Actor, Target, Selection>> selectors) {
        List<EntitySelector<Actor, Target, Selection>> selectorsCopy = new ArrayList<>(selectors);
        ExceptionHelper.checkNotNullElements(selectorsCopy, "selectors");

        int count = selectorsCopy.size();
        if (count == 0) {
            return EntitySelectors.empty();
        }
        if (count == 1) {
            return selectorsCopy.get(0);
        }

        return (World world, Actor actor, Target target) -> {
            Stream<? extends Selection> result = null;
            for (EntitySelector<Actor, Target, Selection> selector: selectorsCopy) {
                Stream<? extends Selection> selected = selector.select(world, actor, target);
                result = result != null
                        ? Stream.concat(result, selected)
                        : selected;
            }
            return result != null ? result : Stream.empty();
        };
    }
}
