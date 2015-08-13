package com.github.kelemen.hearthstone.emulator.actions2;

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
