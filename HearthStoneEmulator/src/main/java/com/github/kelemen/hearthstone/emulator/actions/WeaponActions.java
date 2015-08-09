package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import com.github.kelemen.hearthstone.emulator.weapons.Weapon;

public final class WeaponActions {
    public static final WeaponAction DECREASE_CHARGE = decreaseCharge(1);
    public static final WeaponAction INCREASE_CHARGE = increaseCharge(1);

    public static WeaponAction decreaseCharge(@NamedArg("amount") int amount) {
        if (amount < 0) {
            return increaseCharge(-amount);
        }
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

    public static WeaponAction increaseCharge(@NamedArg("amount") int amount) {
        if (amount < 0) {
            return decreaseCharge(-amount);
        }
        return (World world, Weapon weapon) -> {
            return weapon.increaseCharges(amount);
        };
    }

    private WeaponActions() {
        throw new AssertionError();
    }
}
