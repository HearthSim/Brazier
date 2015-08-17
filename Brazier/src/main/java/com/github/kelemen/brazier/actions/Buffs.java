package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.abilities.HpProperty;
import com.github.kelemen.hearthstone.emulator.actions.ActionUtils;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRef;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionProperties;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.function.Function;

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

    public static PermanentBuff<Minion> setAttack(@NamedArg("attack") int attack) {
        return (World world, Minion target) -> {
            return target.getBuffableAttack().setValueTo(attack);
        };
    }

    private static PermanentBuff<TargetableCharacter> adjustHp(Function<HpProperty, UndoAction> action) {
        return (World world, TargetableCharacter target) -> {
            return ActionUtils.adjustHp(target, action);
        };
    }

    public static PermanentBuff<TargetableCharacter> setCurrentHp(@NamedArg("hp") int hp) {
        return adjustHp((hpProperty) -> {
            if (hpProperty.getMaxHp() >= hp) {
                return hpProperty.setCurrentHp(hp);
            }
            else {
                return hpProperty.setMaxAndCurrentHp(hp);
            }
        });
    }

    private Buffs() {
        throw new AssertionError();
    }
}
