package com.github.kelemen.hearthstone.emulator.actions2;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.ActionUtils;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jtrim.utils.ExceptionHelper;

public final class EntityFilters {
    public static <Entity> EntityFilter<Entity> empty() {
        return (world, entities) -> Stream.empty();
    }

    public static <Entity> EntityFilter<Entity> random() {
        return (World world, Stream<? extends Entity> entities) -> {
            List<Entity> elements = entities.collect(Collectors.<Entity>toList());
            Entity result = ActionUtils.pickRandom(world, elements);
            if (result == null) {
                return Stream.empty();
            }
            else {
                return Stream.of(result);
            }
        };
    }

    public static <Entity> EntityFilter<Entity> random(@NamedArg("count") int count) {
        ExceptionHelper.checkArgumentInRange(count, 0, Integer.MAX_VALUE, "count");
        if (count == 0) {
            return empty();
        }
        if (count == 1) {
            return random();
        }

        return (World world, Stream<? extends Entity> entities) -> {
            List<Entity> elements = entities.collect(Collectors.<Entity>toList());
            return ActionUtils.pickMultipleRandom(world, count, elements).stream();
        };
    }

    private EntityFilters() {
        throw new AssertionError();
    }
}
