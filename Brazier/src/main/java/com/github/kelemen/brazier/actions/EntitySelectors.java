package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.World;
import java.util.stream.Stream;

public final class EntitySelectors {
    public static <Actor, Selection> EntitySelector<Actor, Selection> empty() {
        return (World world, Actor actor) -> Stream.empty();
    }

    private EntitySelectors() {
        throw new AssertionError();
    }
}
