package com.github.kelemen.hearthstone.emulator;

import java.util.function.Predicate;

public interface PlayerPredicate<T> {
    public static final PlayerPredicate<Object> ANY = (playerId, arg) -> true;
    public static final PlayerPredicate<Object> NONE = (playerId, arg) -> false;

    public boolean test(PlayerId playerId, T arg);

    public default PlayerPredicate<T> and(PlayerPredicate<? super T> other) {
        return (playerId, arg) -> this.test(playerId, arg) && other.test(playerId, arg);
    }

    public default Predicate<T> toPredicate(PlayerId playerId) {
        return (arg) -> test(playerId, arg);
    }
}
