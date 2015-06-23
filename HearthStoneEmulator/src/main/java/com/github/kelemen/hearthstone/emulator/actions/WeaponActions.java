package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import com.github.kelemen.hearthstone.emulator.weapons.Weapon;

public final class WeaponActions {
    public static final WeaponAction DECREASE_CHARGE = decreaseCharge(1);

    public static WeaponAction decreaseCharge(@NamedArg("amount") int amount) {
        return (World world, Weapon weapon) -> {
            if (amount == 1) {
                return weapon.decreaseCharges();
            }

            UndoBuilder result = new UndoBuilder(amount);
            for (int i = 0; i < amount; i++) {
                result.addUndo(weapon.decreaseCharges());
            }
            return result;
        };
    }

    private WeaponActions() {
        throw new AssertionError();
    }
}
