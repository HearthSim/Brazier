package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.PlayerProperty;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.parsing.NamedArg;
import com.github.kelemen.brazier.weapons.Weapon;
import java.util.Arrays;
import java.util.Collections;

public final class WeaponAuras {
    public static final AuraTargetProvider<Object, Weapon> WEAPON_PROVIDER = (World world, Object source) -> {
        Weapon weapon1 = world.getPlayer1().tryGetWeapon();
        Weapon weapon2 = world.getPlayer2().tryGetWeapon();
        if (weapon1 == null) {
            return weapon2 != null ? Collections.singletonList(weapon2) : Collections.emptyList();
        }
        else {
            return weapon2 != null ? Arrays.asList(weapon1, weapon2) : Collections.singletonList(weapon1);
        }
    };

    public static final AuraTargetProvider<PlayerProperty, Weapon> OWN_WEAPON_PROVIDER = (World world, PlayerProperty source) -> {
        Weapon weapon = source.getOwner().tryGetWeapon();
        return weapon != null ? Collections.singletonList(weapon) : Collections.emptyList();
    };

    public static Aura<Object, Weapon> attackBuff(@NamedArg("attack") int attack) {
        return (World world, Object source, Weapon target) -> {
            return target.getBuffableAttack().addExternalBuff(attack);
        };
    }

    private WeaponAuras() {
        throw new AssertionError();
    }
}
