package com.github.kelemen.hearthstone.emulator.actions2;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.ActionUtils;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jtrim.utils.ExceptionHelper;

public final class TargetFilters {
    public static <Actor, Target> TargetFilter<Actor, Target> noTarget() {
        return (world, actor, target) -> Stream.empty();
    }

    public static <Actor, Target> TargetFilter<Actor, Target> random() {
        return (World world, Actor actor, Stream<Target> target) -> {
            List<Target> elements = target.collect(Collectors.toList());
            Target result = ActionUtils.pickRandom(world, elements);
            if (result == null) {
                return Stream.empty();
            }
            else {
                return Stream.of(result);
            }
        };
    }

    public static <Actor, Target> TargetFilter<Actor, Target> random(@NamedArg("count") int count) {
        ExceptionHelper.checkArgumentInRange(count, 0, Integer.MAX_VALUE, "count");
        if (count == 0) {
            return noTarget();
        }
        if (count == 1) {
            return random();
        }

        return (World world, Actor actor, Stream<Target> target) -> {
            List<Target> elements = target.collect(Collectors.toList());
            return ActionUtils.pickMultipleRandom(world, count, elements).stream();
        };
    }

    private TargetFilters() {
        throw new AssertionError();
    }
}
