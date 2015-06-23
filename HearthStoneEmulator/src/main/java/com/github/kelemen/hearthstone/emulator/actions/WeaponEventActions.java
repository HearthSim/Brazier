package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import com.github.kelemen.hearthstone.emulator.weapons.Weapon;
import org.jtrim.utils.ExceptionHelper;

public final class WeaponEventActions {
    public static WorldEventAction<Weapon, Object> doSelf(
            @NamedArg("action") WeaponAction action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, Weapon self, Object eventSource) -> {
            return action.alterWorld(world, self);
        };
    }

    private WeaponEventActions() {
        throw new AssertionError();
    }
}
