package com.github.kelemen.hearthstone.emulator.abilities;

import com.github.kelemen.hearthstone.emulator.Silencable;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRef;

public final class AuraAwareIntProperty implements Silencable {
    private final int baseValue;
    private final BuffableIntProperty preAuraBuffs;
    private final BuffableIntProperty ownValue;
    private final BuffableIntProperty auraBuffs;

    public AuraAwareIntProperty(int baseValue) {
        this.baseValue = baseValue;
        this.preAuraBuffs = new BuffableIntProperty(() -> baseValue);
        this.ownValue = new BuffableIntProperty(this.preAuraBuffs::getValue);
        this.auraBuffs = new BuffableIntProperty(this.ownValue::getValue);
    }

    private AuraAwareIntProperty(AuraAwareIntProperty other) {
        this.baseValue = other.baseValue;
        this.preAuraBuffs = new BuffableIntProperty(() -> baseValue);
        this.ownValue = other.ownValue.copy(this.preAuraBuffs::getValue);
        this.auraBuffs = new BuffableIntProperty(this.ownValue::getValue);
    }

    public AuraAwareIntProperty copy() {
        return new AuraAwareIntProperty(this);
    }

    @Override
    public UndoAction silence() {
        return ownValue.silence();
    }

    public UndoAction setValueTo(int newValue) {
        return ownValue.setValueTo(newValue);
    }

    public UndoAction addBuff(int toAdd) {
        return ownValue.addNonRemovableBuff(toAdd);
    }

    public UndoableUnregisterRef addBuff(IntPropertyBuff toAdd) {
        return ownValue.addBuff(toAdd);
    }

    public UndoableUnregisterRef addRemovableBuff(int toAdd) {
        return ownValue.addBuff(toAdd);
    }

    public int getValue() {
        return auraBuffs.getValue();
    }

    public UndoableUnregisterRef addPreAuraBuff(int toAdd) {
        return preAuraBuffs.addBuff(toAdd);
    }

    public UndoableUnregisterRef addPreAuraBuff(IntPropertyBuff buff) {
        return preAuraBuffs.addBuff(buff);
    }

    public UndoableUnregisterRef addAuraBuff(int toAdd) {
        return auraBuffs.addBuff(toAdd);
    }

    public UndoableUnregisterRef addAuraBuff(IntPropertyBuff buff) {
        return auraBuffs.addBuff(buff);
    }
}
