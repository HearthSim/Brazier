package com.github.kelemen.hearthstone.emulator;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jtrim.utils.ExceptionHelper;

public final class Keyword {
    private static final ConcurrentMap<String, Keyword> CACHE = new ConcurrentHashMap<>();

    private final String name;

    private Keyword(String name) {
        this.name = name;
    }

    public static Keyword create(String name) {
        ExceptionHelper.checkNotNullArgument(name, "name");

        String normName = name.toLowerCase(Locale.ROOT);
        return CACHE.computeIfAbsent(normName, (key) -> new Keyword(key));
    }

    public String getName() {
        return name;
    }

    // We don't need equals / hashCode because the factory method does
    // not allow to create different instances with the same name.

    @Override
    public String toString() {
        return name;
    }
}
