package com.github.kelemen.brazier;

import org.jtrim.utils.ExceptionHelper;

public final class PlayerId {
    private final String name;

    public PlayerId(String name) {
        ExceptionHelper.checkNotNullArgument(name, "name");
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Player " + name;
    }
}
