package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.World;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jtrim.utils.ExceptionHelper;

public interface EntitySelector<Actor, Selection> {
    public Stream<? extends Selection> select(World world, Actor actor);

    public default UndoAction forEach(World world, Actor actor, Function<? super Selection, UndoAction> action) {
        UndoBuilder result = new UndoBuilder();
        select(world, actor).forEach((selection) -> {
            result.addUndo(action.apply(selection));
        });
        return result;
    }

    public default TargetedEntitySelector<Actor, Object, Selection> toTargeted() {
        return (World world, Actor actor, Object target) -> select(world, actor);
    }

    public static <Actor, Selection> EntitySelector<Actor, Selection> mergeToCommonBase(
            Collection<EntitySelector<Actor, ? extends Selection>> selectors) {
        List<EntitySelector<Actor, ? extends Selection>> selectorsCopy = new ArrayList<>(selectors);
        ExceptionHelper.checkNotNullElements(selectorsCopy, "selectors");

        return (World world, Actor actor) -> {
            Stream<? extends Selection> result = null;
            for (EntitySelector<Actor, ? extends Selection> selector: selectorsCopy) {
                Stream<? extends Selection> selected = selector.select(world, actor);
                result = result != null
                        ? Stream.concat(result, selected)
                        : selected;
            }
            return result != null ? result : Stream.empty();
        };
    }

    public static <Actor, Selection> EntitySelector<Actor, Selection> merge(
            Collection<? extends EntitySelector<Actor, Selection>> selectors) {
        List<EntitySelector<Actor, Selection>> selectorsCopy = new ArrayList<>(selectors);
        ExceptionHelper.checkNotNullElements(selectorsCopy, "selectors");

        int count = selectorsCopy.size();
        if (count == 0) {
            return EntitySelectors.empty();
        }
        if (count == 1) {
            return selectorsCopy.get(0);
        }

        return (World world, Actor actor) -> {
            Stream<? extends Selection> result = null;
            for (EntitySelector<Actor, Selection> selector: selectorsCopy) {
                Stream<? extends Selection> selected = selector.select(world, actor);
                result = result != null
                        ? Stream.concat(result, selected)
                        : selected;
            }
            return result != null ? result : Stream.empty();
        };
    }
}
