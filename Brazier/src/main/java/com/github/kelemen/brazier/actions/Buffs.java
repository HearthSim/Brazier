package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRef;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionProperties;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;

public final class Buffs {
    public static Buff<TargetableCharacter> addAttack(@NamedArg("attack") int attack) {
        return (World world, TargetableCharacter target) -> {
            if (target instanceof Minion) {
                Minion minion = (Minion)target;
                MinionProperties properties = minion.getProperties();
                return properties.addRemovableAttackBuff(attack);
            }
            else if (target instanceof Hero) {
                Hero hero = (Hero)target;
                return hero.addExtraAttackForThisTurn(attack);
            }
            else {
                return UndoableUnregisterRef.UNREGISTERED_REF;
            }
        };
    }

    private Buffs() {
        throw new AssertionError();
    }
}
