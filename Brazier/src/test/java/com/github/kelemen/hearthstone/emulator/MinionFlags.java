package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.minions.Minion;
import java.util.Set;
import java.util.function.Predicate;

public enum MinionFlags {
    TAUNT((minion) -> minion.getBody().isTaunt()),
    FROZEN((minion) -> minion.getProperties().isFrozen()),
    STEALTH((minion) -> minion.getBody().isStealth()),
    UNTARGETABLE((minion) -> !minion.getBody().isTargetable());

    private final Predicate<? super Minion> propertyGetter;

    private MinionFlags(Predicate<? super Minion> propertyGetter) {
        this.propertyGetter = propertyGetter;
    }

    public boolean getFlag(Minion minion) {
        return propertyGetter.test(minion);
    }

    public static boolean onlyHasFlags(Minion minion, Set<MinionFlags> flags) {
        for (MinionFlags flag: values()) {
            if (flag.getFlag(minion) != flags.contains(flag)) {
                return false;
            }
        }
        return true;
    }
}
