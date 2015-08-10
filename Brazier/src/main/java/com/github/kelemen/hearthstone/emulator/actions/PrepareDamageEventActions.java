package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.DamageRequest;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;

public final class PrepareDamageEventActions {
    public static WorldEventAction<PlayerProperty, DamageRequest> adjustDamage(@NamedArg("damage") int damage) {
        return (World world, PlayerProperty self, DamageRequest eventSource) -> {
            return eventSource.adjustDamage(damage);
        };
    }

    private PrepareDamageEventActions() {
        throw new AssertionError();
    }
}
